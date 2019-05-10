package org.ftoth.general.util.onesec.ivr.impl;

import static org.ftoth.general.util.onesec.ivr.IvrEndpointConversationState.CONNECTING;
import static org.ftoth.general.util.onesec.ivr.IvrEndpointConversationState.INVALID;
import static org.ftoth.general.util.onesec.ivr.IvrEndpointConversationState.READY;
import static org.ftoth.general.util.onesec.ivr.IvrEndpointConversationState.TALKING;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.media.protocol.FileTypeDescriptor;
import javax.telephony.Address;
import javax.telephony.Connection;
import javax.telephony.TerminalConnection;
import javax.telephony.TerminalObserver;
import javax.telephony.callcontrol.CallControlCall;
import javax.telephony.callcontrol.CallControlConnection;
import javax.telephony.events.TermEv;
import javax.telephony.media.MediaTerminalConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.jtapi.CallControlConnectionUtil;
import org.ftoth.general.util.jtapi.ConnectionUtil;
import org.ftoth.general.util.onesec.Codec;
import org.ftoth.general.util.onesec.ivr.AudioStream;
import org.ftoth.general.util.onesec.ivr.BufferCache;
import org.ftoth.general.util.onesec.ivr.CodecManager;
import org.ftoth.general.util.onesec.ivr.CompletionCode;
import org.ftoth.general.util.onesec.ivr.IncomingRtpStream;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversation;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationEvent;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationException;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationListener;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationRtpStateException;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationState;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationStateException;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationStoppedEvent;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationTransferedEvent;
import org.ftoth.general.util.onesec.ivr.OutgoingRtpStream;
import org.ftoth.general.util.onesec.ivr.RtpStreamException;
import org.ftoth.general.util.onesec.ivr.RtpStreamManager;

import com.cisco.jtapi.extensions.CiscoCall;
import com.cisco.jtapi.extensions.CiscoTermInServiceEv;
import com.cisco.jtapi.extensions.CiscoTerminal;

public class IvrEndpointConversationImpl implements IvrEndpointConversation
{
	private static final Log log = LogFactory.getLog(IvrEndpointConversationImpl.class);

	private enum RtpStatus {
		INVALID, CREATED, CONNECTED, WAITING_FOR_START
	}

	private BufferCache bufferCache;

	private CodecManager codecManager;

	private final RtpStreamManager streamManager;

	private final boolean enableIncomingRtpStream;
	private final AtomicBoolean audioStreamJustCreated = new AtomicBoolean();
	private final String terminalAddress;
	private Map<String, Object> additionalBindings;
	private Codec codec;
	private int packetSize;
	private int maxSendAheadPacketsCount;

	private OutgoingRtpStream outRtp;
	private IncomingRtpStream inRtp;
	private RtpStatus inRtpStatus = RtpStatus.INVALID;
	private RtpStatus outRtpStatus = RtpStatus.INVALID;
	/*
	 * private ConversationScenarioState conversationState; private
	 * IvrActionsExecutor actionsExecutor;
	 */
	//private ConcatDataSource audioStream;
	private AudioStream audioStream;
	private CiscoCall call;
	private String remoteAddress;
	private int remotePort;
	private String callId;
	// private BindingSupportImpl bindingSupport;
	private IvrEndpointConversationStateImpl state;
	private String callingNumber;
	private String calledNumber;
	private Collection<IvrEndpointConversationListener> listeners;
	private volatile boolean stopping = false;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	public IvrEndpointConversationImpl(RtpStreamManager streamManager, boolean enableIncomingRtpStream, String terminalAddress, Map<String, Object> additionalBindings) throws Exception
	{
		this.streamManager = streamManager;
		this.additionalBindings = additionalBindings;
		this.enableIncomingRtpStream = enableIncomingRtpStream;
		this.terminalAddress = terminalAddress;

		bufferCache = streamManager.getBufferCache();
		codecManager = streamManager.getCodecManager();

		state = new IvrEndpointConversationStateImpl(this);
		state.setState(INVALID);
		// stateListenersCoordinator.addListenersToState(state,
		// IvrEndpointConversationState.class);
	}

	public void addConversationListener(IvrEndpointConversationListener listener)
	{
		lock.writeLock().lock();
		try {
			if (listeners == null)
				listeners = new HashSet<IvrEndpointConversationListener>();
			listeners.add(listener);
			listener.listenerAdded(new IvrEndpointConversationEventImpl(this));
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	public void removeConversationListener(IvrEndpointConversationListener listener)
	{
		lock.writeLock().lock();
		try {
			if (listeners != null)
				listeners.remove(listener);
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	public String getCallingNumber()
	{
		return callingNumber;
	}

	public String getCalledNumber()
	{
		return calledNumber;
	}

	public IvrEndpointConversationState getState()
	{
		return state;
	}

	private void checkState()
			throws IvrEndpointConversationException
	{
		switch (state.getId()) {
		case INVALID:
			if (call != null)
				state.setState(READY);
			break;
		case READY:
			if (call == null)
				state.setState(INVALID);
			else if (inRtpStatus == RtpStatus.CREATED || outRtpStatus == RtpStatus.CREATED)
				state.setState(CONNECTING);
			break;
		case CONNECTING:
			if (call == null) {
				stopIncomingRtp();
				stopOutgoingRtp();
				state.setState(INVALID);
			}
			else if (inRtpStatus == RtpStatus.CONNECTED && outRtpStatus == RtpStatus.CONNECTED) {
				if (isAllLogicalConnectionEstablished()) {
					state.setState(TALKING);
					fireEvent(true, null);
					startConversation();
				}
			}
			else if (inRtpStatus == RtpStatus.WAITING_FOR_START || outRtpStatus == RtpStatus.WAITING_FOR_START && isAllLogicalConnectionEstablished()) {
				if (inRtpStatus == RtpStatus.WAITING_FOR_START && outRtpStatus.ordinal() > RtpStatus.CREATED.ordinal()) {
					startIncomingRtp();
				}
				if (outRtpStatus == RtpStatus.WAITING_FOR_START) {
					startOutgoingRtp();
				}
			}
			// else if (inRtpStatus==RtpStatus.WAITING_FOR_START &&
			// outRtpStatus.ordinal()>=RtpStatus.CREATED.ordinal())
			// startIncomingRtp();
			else if (inRtpStatus == RtpStatus.INVALID && outRtpStatus == RtpStatus.INVALID)
				state.setState(READY);
			break;
		case TALKING:
			if (call == null) {
				state.setState(CONNECTING);
				checkState();
			}
			else if (inRtpStatus != RtpStatus.CONNECTED || outRtpStatus != RtpStatus.CONNECTED)
				state.setState(CONNECTING);
			break;
		}
	}

	public void setCall(CallControlCall call)
			throws IvrEndpointConversationException
	{
		lock.writeLock().lock();
		try {
			if (state.getId() != INVALID)
				throw new IvrEndpointConversationStateException("Can't setCall", "INVALID", state.getIdName());
			this.call = (CiscoCall) call;
			callId = "[call id: " + this.call.getCallID().intValue() + ", calling number: " + call.getCallingAddress().getName() + "]";
			callingNumber = getPartyNumber(true);
			calledNumber = getPartyNumber(false);
			checkState();
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	public IncomingRtpStream initIncomingRtp()
			throws IvrEndpointConversationException
	{
		lock.writeLock().lock();
		try {
			if (state.getId() != READY && state.getId() != CONNECTING) {
				throw new IvrEndpointConversationStateException("Can't init incoming RTP", "READY, CONNECTING", state.getIdName());
			}
			if (inRtpStatus != RtpStatus.INVALID) {
				throw new IvrEndpointConversationRtpStateException("Can't create incoming RTP stream", "INVALID", inRtpStatus.name());
			}
			inRtp = streamManager.getIncomingRtpStream(RtpStreamManagerImpl.DUMMY_OWNER_NAME);
			inRtp.setLogPrefix(callId + " : ");
			inRtpStatus = RtpStatus.CREATED;
			if (log.isDebugEnabled()) {
				log.debug(callLog("Incoming RTP successfully created"));
			}
			checkState();
			return inRtp;
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	public void initOutgoingRtp(String remoteAddress, int remotePort, int packetSize, int maxSendAheadPacketsCount, Codec codec)
			throws IvrEndpointConversationException
	{
		lock.writeLock().lock();
		try {
			if (state.getId() != READY && state.getId() != CONNECTING)
				throw new IvrEndpointConversationStateException("Can't init outgoing RTP", "READY, CONNECTING", state.getIdName());
			if (outRtpStatus != RtpStatus.INVALID)
				throw new IvrEndpointConversationRtpStateException("Can't create outgoing RTP stream", "INVALID", outRtpStatus.name());
			this.remoteAddress = remoteAddress;
			this.remotePort = remotePort;
			this.packetSize = packetSize;
			this.maxSendAheadPacketsCount = maxSendAheadPacketsCount;
			this.codec = codec;
			outRtp = streamManager.getOutgoingRtpStream(RtpStreamManagerImpl.DUMMY_OWNER_NAME);
			outRtp.setLogPrefix(callId + " : ");
			outRtpStatus = RtpStatus.CREATED;
			if (log.isDebugEnabled()) {
				log.debug(callLog("Outgoing RTP successfully created"));
			}
			checkState();
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	public void logicalConnectionCreated(String opponentNumber)
			throws IvrEndpointConversationException
	{
		lock.writeLock().lock();
		try {
			if (log.isDebugEnabled()) {
				log.debug(callLog("Logical connection created for opponent number (%s)", opponentNumber));
			}
			checkForOpponentPartyTransfered(opponentNumber);
			if (state.getId() == CONNECTING)
				checkState();
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	private void checkForOpponentPartyTransfered(String opponentNumber)
	{
		/*
		 * if (!ObjectUtils.in(opponentNumber, callingNumber, calledNumber)) {
		 * if (log.isDebugEnabled()) {
		 * log.debug(callLog("Call transfered to number (%s)", opponentNumber));
		 * } fireTransferedEvent(opponentNumber); }
		 */
	}

	private String getPartyNumber(boolean callingParty)
	{
		Address addr = callingParty ? call.getCallingAddress() : call.getCalledAddress();
		return addr == null ? null : addr.getName();
	}

	private boolean isAllLogicalConnectionEstablished()
	{
		Connection[] cons = call.getConnections();
		if (cons != null) {
			if (log.isDebugEnabled()) {
				for (Connection con : cons) {
					String connState = ConnectionUtil.getStateName(con.getState());
					String callCtrlState = CallControlConnectionUtil.getStateName(((CallControlConnection) con).getCallControlState());
					log.debug(callLog("Call connection: address=%s; state=%s; callControlState=%s", con.getAddress().getName(), connState, callCtrlState));
				}
			}
			for (Connection con : cons)
				if (((CallControlConnection) con).getCallControlState() != CallControlConnection.ESTABLISHED)
					return false;
			return true;
		}
		return false;
	}

	public void startIncomingRtp()
			throws IvrEndpointConversationException
	{
		lock.writeLock().lock();
		try {
			if (log.isDebugEnabled()) {
				log.debug(callLog("Trying to start incoming RTP stream"));
			}
			if (state.getId() != CONNECTING)
				throw new IvrEndpointConversationStateException("Can't start incoming RTP", "CONNECTING", state.getIdName());
			if (inRtpStatus != RtpStatus.CREATED && inRtpStatus != RtpStatus.WAITING_FOR_START)
				throw new IvrEndpointConversationRtpStateException("Can't start incoming RTP stream", "CREATED, WATING_FOR_START", inRtpStatus.name());
			try {
				// if (outRtpStatus.ordinal()>=RtpStatus.CREATED.ordinal() &&
				// isAllLogicalConnectionEstablished()) {
				if (outRtpStatus.ordinal() >= RtpStatus.CREATED.ordinal()) {
					if (enableIncomingRtpStream)
						inRtp.open(remoteAddress);
					fireIncomingRtpStartedEvent();
					inRtpStatus = RtpStatus.CONNECTED;
					checkState();
				}
				else {
					if (log.isDebugEnabled()) {
						log.debug(callLog("Incoming RTP. Can't start. Outgoing RTP not created, waiting..."));
					}
					inRtpStatus = RtpStatus.WAITING_FOR_START;
				}
			}
			catch (RtpStreamException e) {
				log.error(callLog("Error starting incoming RTP"), e);
				stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
			}
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	public void startOutgoingRtp()
			throws IvrEndpointConversationException
	{
		lock.writeLock().lock();
		try {
			if (log.isDebugEnabled()) {
				log.debug(callLog("Trying to start outgoing RTP stream"));
			}
			if (state.getId() != CONNECTING) {
				throw new IvrEndpointConversationStateException("Can't start incoming RTP", "CONNECTING", state.getIdName());
			}
			if (outRtpStatus != RtpStatus.CREATED && outRtpStatus != RtpStatus.WAITING_FOR_START) {
				throw new IvrEndpointConversationRtpStateException("Can't start incoming RTP stream", "CREATED, WAITING_FOR_START", outRtpStatus.name());
			}
			try {
				if (isAllLogicalConnectionEstablished()) {
					if (log.isDebugEnabled()) {
						log.debug(callLog("Packet size:" + packetSize + ", maxSendAheadPacketsCount:" + maxSendAheadPacketsCount));
					}
					audioStream = new ConcatDataSource(FileTypeDescriptor.WAVE, codecManager, codec, packetSize, 0, maxSendAheadPacketsCount, bufferCache);
					//audioStream = new TestAudioStream(codecManager, "/org/ftoth/general/onesec/ivr/tada.wav");
					//audioStream.setLogPrefix(callId + " : ");
					audioStreamJustCreated.set(true);
					outRtp.open(remoteAddress, remotePort, audioStream);
					outRtp.start();
					outRtpStatus = RtpStatus.CONNECTED;
					fireOutgoingRtpStartedEvent();
					checkState();
				}
				else {
					if (log.isDebugEnabled()) {
						log.debug(callLog("Outgoing RTP. Can't start. " + "Not all logical connections are established. Waiting..."));
					}
					outRtpStatus = RtpStatus.WAITING_FOR_START;
				}
			}
			catch (Exception e) {
				log.error(callLog("Error starting outgoing RTP"), e);
				stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
			}
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	public void stopIncomingRtp()
	{
		lock.writeLock().lock();
		try {
			try {
				if (inRtp != null) {
					inRtp.release();
					inRtp = null;
					inRtpStatus = RtpStatus.INVALID;
					checkState();
				}
			}
			catch (Throwable e) {
				log.warn("Problem with stopping incoming rtp stream", e);
			}
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	public void stopOutgoingRtp()
	{
		lock.writeLock().lock();
		try {
			try {
				if (outRtp != null) {
					/*
					 * if (actionsExecutor != null)
					 * actionsExecutor.cancelActionsExecution();
					 */
					outRtp.release();
					outRtp = null;
					outRtpStatus = RtpStatus.INVALID;
					if (audioStream != null) {
						audioStream.close();
						audioStream = null;
					}
					// actionsExecutor = null;
					checkState();
				}
			}
			catch (Throwable e) {
				log.warn("Problem with stopping outgoing rtp stream", e);
			}
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	private void startConversation()
			throws IvrEndpointConversationException
	{
		try {
			boolean initialized = initConversation();
			if (log.isDebugEnabled()) {
				log.debug(callLog("Conversation %s", initialized ? "started" : "restarted"));
			}
			continueConversation(EMPTY_DTMF);
		}
		catch (Throwable e) {
			stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
		}

	}

	private boolean initConversation()
			throws Exception
	{
		/*
		 * if (conversationState == null) { conversationState =
		 * scenario.createConversationState();
		 * conversationState.setBinding(DTMF_BINDING, "-",
		 * BindingScope.CONVERSATION);
		 * conversationState.setBindingDefaultValue(DTMF_BINDING, "-");
		 * conversationState.setBinding(VARS_BINDING, new HashMap(),
		 * BindingScope.CONVERSATION);
		 * conversationState.setBinding(CONVERSATION_STATE_BINDING,
		 * conversationState, BindingScope.CONVERSATION);
		 * conversationState.setBinding(NUMBER_BINDING, callingNumber,
		 * BindingScope.CONVERSATION);
		 * conversationState.setBinding(CALLED_NUMBER_BINDING, calledNumber,
		 * BindingScope.CONVERSATION); if (additionalBindings != null) for
		 * (Map.Entry<String, Object> b : additionalBindings.entrySet())
		 * conversationState.setBinding(b.getKey(), b.getValue(),
		 * BindingScope.CONVERSATION); additionalBindings = null;
		 * actionsExecutor = new IvrActionsExecutor(this, executor);
		 * actionsExecutor.setLogPrefix(callId + " : "); this.bindingSupport =
		 * new BindingSupportImpl(); return true; }
		 */
		return false;
	}

	/*
	 * public void continueConversation(char dtmfChar) {
	 * lock.writeLock().lock(); try { try { if (dtmfChar != EMPTY_DTMF)
	 * fireDtmfReceived(dtmfChar); if (IvrEndpointConversationState.TALKING !=
	 * state.getId()) { if (owner.isLogLevelEnabled(LogLevel.DEBUG))
	 * owner.getLogger().debug( callLog("Can't continue conversation. " +
	 * "Conversation is not started. " + "Current conversation state is %s",
	 * state.getIdName())); return; }
	 * 
	 * IvrConversationScenarioPoint point = (IvrConversationScenarioPoint)
	 * conversationState.getConversationPoint(); String validDtmfs =
	 * point.getValidDtmfs(); if (dtmfChar != EMPTY_DTMF &&
	 * (conversationState.isDtmfProcessingDisabled() || validDtmfs == null ||
	 * validDtmfs.indexOf(dtmfChar) < 0)) { if
	 * (owner.isLogLevelEnabled(LogLevel.DEBUG))
	 * owner.getLogger().debug(callLog("Invalid dtmf (%s). Skipping",
	 * dtmfChar)); return; }
	 * 
	 * if (actionsExecutor.hasDtmfProcessPoint(dtmfChar)) { if
	 * (owner.isLogLevelEnabled(LogLevel.DEBUG))
	 * owner.getLogger().debug("Collecting DTMF chars. Collected: " +
	 * actionsExecutor.getCollectedDtmfs().toString()); return; }
	 * 
	 * conversationState.enableDtmfProcessing();
	 * 
	 * if (owner.isLogLevelEnabled(LogLevel.DEBUG))
	 * owner.getLogger().debug(callLog("Continue conversation using dtmf (" +
	 * dtmfChar + ")"));
	 * 
	 * if (!audioStreamJustCreated.compareAndSet(true, false))
	 * audioStream.reset(); conversationState.getBindings().put(DTMF_BINDING, ""
	 * + dtmfChar); Collection<Node> actions =
	 * scenario.makeConversation(conversationState); Collection<IvrAction>
	 * ivrActions = new ArrayList<IvrAction>(10); String bindingId = null; try {
	 * bindingId = tree.addGlobalBindings(bindingSupport);
	 * bindingSupport.putAll(conversationState.getBindings());
	 * bindingSupport.put(DTMF_BINDING, "" + dtmfChar); for (Node node :
	 * actions) if (node instanceof IvrActionNode) { IvrAction action =
	 * ((IvrActionNode) node).createAction(); if (action != null)
	 * ivrActions.add(action); } else if (node instanceof GotoNode || node
	 * instanceof ConversationScenarioPoint) ivrActions.add(new
	 * ContinueConversationAction());
	 * actionsExecutor.executeActions(ivrActions); } finally { if (bindingId !=
	 * null) tree.removeGlobalBindings(bindingId); bindingSupport.reset(); } }
	 * catch (Exception e) { if (owner.isLogLevelEnabled(LogLevel.ERROR))
	 * owner.getLogger
	 * ().error(callLog("Error continue conversation using dtmf %s", dtmfChar),
	 * e); } } finally { lock.writeLock().unlock(); } }
	 * 
	 * public void stopConversation(CompletionCode completionCode) {
	 * lock.writeLock().lock(); try { if (state.getId() == INVALID || stopping)
	 * return; stopping = true; if (state.getId() == TALKING || state.getId() ==
	 * CONNECTING || state.getId() == READY) dropCallConnections(); call = null;
	 * try { checkState(); } catch (IvrEndpointConversationException e) { if
	 * (owner.isLogLevelEnabled(LogLevel.WARN))
	 * owner.getLogger().warn(callLog("Problem with stopping conversation"), e);
	 * } if (owner.isLogLevelEnabled(LogLevel.DEBUG))
	 * owner.getLogger().debug(callLog("Conversation stopped (%s)",
	 * completionCode)); } finally { lock.writeLock().unlock(); }
	 * fireEvent(false, completionCode); }
	 * 
	 * private void dropCallConnections() { try { if (call.getState() ==
	 * Call.ACTIVE) { if (owner.isLogLevelEnabled(LogLevel.DEBUG))
	 * owner.getLogger().debug(callLog("Dropping the call")); Connection[]
	 * connections = call.getConnections(); if (connections != null &&
	 * connections.length > 0) for (Connection connection : connections) { if
	 * (owner.isLogLevelEnabled(LogLevel.DEBUG)) owner.getLogger()
	 * .debug(callLog("Disconnecting connection for address (%s)",
	 * connection.getAddress().getName())); if (((CiscoAddress)
	 * connection.getAddress()).getState() == CiscoAddress.IN_SERVICE)
	 * connection.disconnect(); else if (owner.getLogger().isDebugEnabled())
	 * owner
	 * .getLogger().debug(callLog("Can't disconnect address not IN_SERVICE")); }
	 * long ts = System.currentTimeMillis(); while (call.getState() ==
	 * Call.ACTIVE) { if (ts + 5000 < System.currentTimeMillis()) throw new
	 * Exception("Timeout while waiting for call drop");
	 * TimeUnit.MILLISECONDS.sleep(10); } } } catch (Throwable e) { if
	 * (owner.isLogLevelEnabled(LogLevel.WARN))
	 * owner.getLogger().warn(callLog("Can't drop call connections"), e);
	 * 
	 * } }
	 */

	/*
	 * public void sendMessage(String message, String encoding,
	 * SendMessageDirection direction) { try { String address = direction ==
	 * SendMessageDirection.CALLED_PARTY ? getCalledNumber() :
	 * getCallingNumber(); ProviderController controller =
	 * providerRegistry.getProviderController(address); CiscoTerminal term =
	 * (CiscoTerminal)
	 * controller.getProvider().getAddress(address).getTerminals()[0];
	 * term.addObserver(new SendTerminalObserver(message, encoding)); } catch
	 * (Throwable e) { if (owner.isLogLevelEnabled(LogLevel.WARN))
	 * owner.getLogger().warn(callLog("Can't send message to %s", direction),
	 * e); } }
	 */

	public void sendDTMF(String digits)
	{
		try {
			Connection[] cons = call.getConnections();
			MediaTerminalConnection termCon = null;
			if (cons != null)
				for (Connection con : cons)
					if (terminalAddress.equals(con.getAddress().getName())) {
						TerminalConnection[] termCons = con.getTerminalConnections();
						if (termCons != null && termCons.length > 0)
							termCon = (MediaTerminalConnection) termCons[0];
					}
			if (termCon == null)
				throw new Exception("Not found terminal connection");
			termCon.generateDtmf(digits);
		}
		catch (Throwable e) {
			log.warn(callLog("Error sending DTMF (%s)", digits), e);
		}
	}

	/*
	 * public ConversationScenarioState getConversationScenarioState() { return
	 * conversationState; }
	 * 
	 * public Node getOwner() { return owner; }
	 * 
	 * public ExecutorService getExecutorService() { return executor; }
	 */

	public AudioStream getAudioStream()
	{
		return audioStream;
	}

	public IncomingRtpStream getIncomingRtpStream()
	{
		return inRtp;
	}

	public void transfer(String address, boolean monitorTransfer, long callStartTimeout, long callEndTimeout)
	{
		lock.writeLock().lock();
		try {
			if (state.getId() != TALKING) {
				log.warn(callLog("Can't transfer call to the address (%s). Invalid call state (%s)", address, state.getIdName()));
				return;
			}
			try {
				audioStream.reset();
				try {
					call.transfer(address);
					fireTransferedEvent(address);
				}
				catch (Exception ex) {
					log.error(callLog("Error transferring call to the address %s", address), ex);
				}
			}
			finally {
				stopConversation(CompletionCode.COMPLETED_BY_OPPONENT);
			}
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	private String callLog(String mess, Object... args)
	{
		return callId + " : Conversation. " + String.format(mess, args);
	}

	public String getObjectName()
	{
		return callId;
	}

	public String getObjectDescription()
	{
		return callId;
	}

	@Override
	public String toString()
	{
		return callId;
	}

	private void fireEvent(boolean conversationStartEvent, CompletionCode completionCode)
	{
		if (listeners != null && !listeners.isEmpty()) {
			IvrEndpointConversationEvent event = conversationStartEvent ? new IvrEndpointConversationEventImpl(this) : new IvrEndpointConversationStoppedEventImpl(this, completionCode);
			for (IvrEndpointConversationListener listener : listeners)
				if (conversationStartEvent)
					listener.conversationStarted(event);
				else
					listener.conversationStopped((IvrEndpointConversationStoppedEvent) event);
		}
	}

	private void fireTransferedEvent(String address)
	{
		if (listeners != null && !listeners.isEmpty()) {
			final IvrEndpointConversationTransferedEvent event = new IvrEndpointConversationTransferedEventImpl(this, address);
			/*
			 * executor.executeQuietly(new AbstractTask(owner,
			 * "Sending conversation transfered event") {
			 * 
			 * @Override public void doRun() throws Exception {
			 */
			for (IvrEndpointConversationListener listener : listeners) {
				listener.conversationTransfered(event);
			}
			/*
			 * } });
			 */
		}
	}

	private void fireDtmfReceived(char dtmf)
	{
		/*
		 * if (listeners != null && !listeners.isEmpty()) { final
		 * IvrDtmfReceivedConversationEventImpl event = new
		 * IvrDtmfReceivedConversationEventImpl(this, dtmf);
		 * executor.executeQuietly(new AbstractTask(owner,
		 * "Sending dtmf received event") {
		 * 
		 * @Override public void doRun() throws Exception { for
		 * (IvrEndpointConversationListener listener : listeners)
		 * listener.dtmfReceived(event); } }); }
		 */
	}

	private void fireIncomingRtpStartedEvent()
	{
		if (listeners != null && !listeners.isEmpty()) {
			IvrIncomingRtpStartedEventImpl ev = new IvrIncomingRtpStartedEventImpl(this);
			for (IvrEndpointConversationListener listener : listeners)
				listener.incomingRtpStarted(ev);
		}
	}

	private void fireOutgoingRtpStartedEvent()
	{
		if (listeners != null && !listeners.isEmpty()) {
			IvrOutgoingRtpStartedEventImpl ev = new IvrOutgoingRtpStartedEventImpl(this);
			for (IvrEndpointConversationListener listener : listeners)
				listener.outgoingRtpStarted(ev);
		}
	}

	private class SendTerminalObserver implements TerminalObserver
	{
		private final String message;
		private final String encoding;

		public SendTerminalObserver(String message, String encoding)
		{
			this.message = String.format("<CiscoIPPhoneText><Text>%s</Text></CiscoIPPhoneText>", message);
			// this.message =
			// String.format("<CiscoIPPhoneStatus><Text>%s</Text></CiscoIPPhoneStatus>",
			// message);
			this.encoding = encoding;
		}

		public void terminalChangedEvent(TermEv[] eventList)
		{
			for (TermEv ev : eventList) {
				if (ev.getID() == CiscoTermInServiceEv.ID) {
					try {
						try {
							((CiscoTerminal) ev.getTerminal()).sendData(message.getBytes(encoding));
						}
						finally {
							ev.getTerminal().removeObserver(this);
						}
					}
					catch (Exception ex) {
						log.warn(callLog("Can't send message"), ex);
					}
				}
			}
		}
	}

	@Override
	public ExecutorService getExecutorService()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void continueConversation(char dtmfChar)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void stopConversation(CompletionCode completionCode)
	{
		// TODO Auto-generated method stub

	}
}
