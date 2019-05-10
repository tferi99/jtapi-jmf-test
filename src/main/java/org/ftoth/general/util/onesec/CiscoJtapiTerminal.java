package org.ftoth.general.util.onesec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.telephony.Address;
import javax.telephony.AddressObserver;
import javax.telephony.Call;
import javax.telephony.Provider;
import javax.telephony.Terminal;
import javax.telephony.callcontrol.CallControlCall;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.callcontrol.CallControlConnection;
import javax.telephony.callcontrol.events.CallCtlConnEstablishedEv;
import javax.telephony.callcontrol.events.CallCtlConnFailedEv;
import javax.telephony.callcontrol.events.CallCtlConnOfferedEv;
import javax.telephony.events.AddrEv;
import javax.telephony.events.CallActiveEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.CallInvalidEv;
import javax.telephony.events.ConnConnectedEv;
import javax.telephony.events.ConnDisconnectedEv;
import javax.telephony.events.TermConnRingingEv;
import javax.telephony.events.TermEv;
import javax.telephony.media.MediaCallObserver;
import javax.telephony.media.events.MediaTermConnDtmfEv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.onesec.ivr.CompletionCode;
import org.ftoth.general.util.onesec.ivr.IncomingRtpStream;
import org.ftoth.general.util.onesec.ivr.IvrDtmfReceivedConversationEvent;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationEvent;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationException;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationListener;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationState;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationStoppedEvent;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationTransferedEvent;
import org.ftoth.general.util.onesec.ivr.IvrIncomingRtpStartedEvent;
import org.ftoth.general.util.onesec.ivr.IvrOutgoingRtpStartedEvent;
import org.ftoth.general.util.onesec.ivr.IvrTerminal;
import org.ftoth.general.util.onesec.ivr.IvrTerminalState;
import org.ftoth.general.util.onesec.ivr.RtpStreamManager;
import org.ftoth.general.util.onesec.ivr.impl.IvrEndpointConversationImpl;
import org.ftoth.general.util.onesec.ivr.impl.IvrEndpointConversationStoppedEventImpl;
import org.ftoth.general.util.onesec.ivr.impl.IvrTerminalStateImpl;

import com.cisco.jtapi.extensions.CiscoAddrInServiceEv;
import com.cisco.jtapi.extensions.CiscoAddrOutOfServiceEv;
import com.cisco.jtapi.extensions.CiscoCall;
import com.cisco.jtapi.extensions.CiscoConnection;
import com.cisco.jtapi.extensions.CiscoMediaOpenLogicalChannelEv;
import com.cisco.jtapi.extensions.CiscoMediaTerminal;
import com.cisco.jtapi.extensions.CiscoRTPInputStartedEv;
import com.cisco.jtapi.extensions.CiscoRTPInputStoppedEv;
import com.cisco.jtapi.extensions.CiscoRTPOutputProperties;
import com.cisco.jtapi.extensions.CiscoRTPOutputStartedEv;
import com.cisco.jtapi.extensions.CiscoRTPOutputStoppedEv;
import com.cisco.jtapi.extensions.CiscoRTPParams;
import com.cisco.jtapi.extensions.CiscoRouteTerminal;
import com.cisco.jtapi.extensions.CiscoTermInServiceEv;
import com.cisco.jtapi.extensions.CiscoTermOutOfServiceEv;
import com.cisco.jtapi.extensions.CiscoTerminal;
import com.cisco.jtapi.extensions.CiscoTerminalObserver;
import com.cisco.jtapi.extensions.CiscoTransferEndEv;

public class CiscoJtapiTerminal implements CiscoTerminalObserver, AddressObserver, CallControlCallObserver, MediaCallObserver, IvrEndpointConversationListener
{
	private static final Log log = LogFactory.getLog(CiscoJtapiTerminal.class);

	private Address termAddress;
	private CiscoTerminal ciscoTerm;
	private int maxChannels = Integer.MAX_VALUE;
	private final AtomicBoolean stopping = new AtomicBoolean();
	private Codec codec;
	private boolean termInService = false;
	private boolean termAddressInService = false;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Map<Integer, ConvHolder> connIds = new HashMap<Integer, ConvHolder>();
	//private final IvrTerminal term;
	private boolean enableIncomingRtp = true;
    private boolean enableIncomingCalls = true;
    private final Set<IvrEndpointConversationListener> conversationListeners = new HashSet<IvrEndpointConversationListener>();
    private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();
    private final Map<Call, ConvHolder> calls = new HashMap<Call, ConvHolder>();
    private final Integer rtpPacketSize;
    private final int rtpMaxSendAheadPacketsCount;
	Provider provider;

	// ------------------------- properties -------------------------
	private String address;
	private final RtpStreamManager rtpStreamManager;
	private final IvrTerminalStateImpl state;

	public Provider getProvider()
	{
		return provider;
	}

	public String getAddress()
	{
		return address;
	}

	
	public RtpStreamManager getRtpStreamManager()
	{
		return rtpStreamManager;
	}

    public IvrTerminalState getState()
    {
        return state;
    }
    
	// ------------------------- startup -------------------------
    public CiscoJtapiTerminal(Provider provider, IvrTerminal term)
    {
    	this.provider = provider;
    	this.rtpStreamManager = term.getRtpStreamManager();
        this.address = term.getAddress();
        this.codec = term.getCodec();
        this.rtpPacketSize = term.getRtpPacketSize();
        this.rtpMaxSendAheadPacketsCount = term.getRtpMaxSendAheadPacketsCount();
        this.enableIncomingRtp = term.getEnableIncomingRtp();
        this.enableIncomingCalls = term.getEnableIncomingCalls();
        //this.term = term;
        this.state = new IvrTerminalStateImpl(term);
/*        stateListenersCoordinator.addListenersToState(state, IvrTerminalState.class);
        this.state.setState(IvrTerminalState.OUT_OF_SERVICE);*/
    }
	
	
	// ------------------------- implements -------------------------
	@Override
	public void terminalChangedEvent(TermEv[] events)
	{
		if (log.isDebugEnabled()) {
			log.debug("Recieved terminal events: " + eventsToString(events));
		}
		for (TermEv ev : events)
			switch (ev.getID()) {
			case CiscoTermInServiceEv.ID:
				termInService = true;
				checkState();
				break;
			case CiscoTermOutOfServiceEv.ID:
				termInService = false;
				checkState();
				break;
			case CiscoMediaOpenLogicalChannelEv.ID:
				initInRtp((CiscoMediaOpenLogicalChannelEv) ev);
				break;
			case CiscoRTPOutputStartedEv.ID:
				initAndStartOutRtp((CiscoRTPOutputStartedEv) ev);
				break;
			case CiscoRTPInputStartedEv.ID:
				startInRtp((CiscoRTPInputStartedEv) ev);
				break;
			case CiscoRTPOutputStoppedEv.ID:
				stopOutRtp((CiscoRTPOutputStoppedEv) ev);
				break;
			case CiscoRTPInputStoppedEv.ID:
				stopInRtp((CiscoRTPInputStoppedEv) ev);
				break;
			}
	}

	@Override
	public void callChangedEvent(CallEv[] events)
	{
		if (log.isDebugEnabled()) {
			log.debug("Recieved call events: " + eventsToString(events));
		}
		for (CallEv ev : events)
			switch (ev.getID()) {
			case CallActiveEv.ID:
				createConversation(((CallActiveEv) ev).getCall());
				break;
			case ConnConnectedEv.ID:
				bindConnIdToConv((ConnConnectedEv) ev);
				break;
			case CallCtlConnOfferedEv.ID:
				acceptIncomingCall((CallCtlConnOfferedEv) ev);
				break;
			case TermConnRingingEv.ID:
				answerOnIncomingCall((TermConnRingingEv) ev);
				break;
			case CallCtlConnEstablishedEv.ID:
				openLogicalChannel((CallCtlConnEstablishedEv) ev);
				break;
			case MediaTermConnDtmfEv.ID:
				continueConv((MediaTermConnDtmfEv) ev);
				break;
			case CiscoTransferEndEv.ID:
				callTransfered((CiscoTransferEndEv) ev);
			case ConnDisconnectedEv.ID:
				unbindConnIdFromConv((ConnDisconnectedEv) ev);
				break;
			case CallCtlConnFailedEv.ID:
				handleConnFailedEvent((CallCtlConnFailedEv) ev);
				break;
			case CallInvalidEv.ID:
				stopConversation(ev.getCall(), CompletionCode.COMPLETED_BY_OPPONENT);
				break;
			}
	}

	@Override
	public void addressChangedEvent(AddrEv[] events)
	{
        if (log.isDebugEnabled()) {
            log.debug("Recieved address events: "+eventsToString(events));
        }
        for (AddrEv ev: events)
            switch (ev.getID()) {
                case CiscoAddrInServiceEv.ID: termAddressInService = true; checkState(); break;
                case CiscoAddrOutOfServiceEv.ID: termAddressInService = false; checkState(); break;
            }
	}

	// ------------------------- action -------------------------
	public void start()
	{
		try {
			if (log.isDebugEnabled()) {
				log.debug("Checking provider...");
			}

			if (provider == null || provider.getState() != Provider.IN_SERVICE) {
				throw new Exception(String.format("Provider is not IN_SERVICE"));
			}
			if (log.isDebugEnabled()) {
				log.debug("Checking terminal address...");
			}

			termAddress = provider.getAddress(address);
			ciscoTerm = registerTerminal(termAddress);
			registerTerminalListeners();
		}
		catch (Throwable e) {
			throw new RuntimeException("Problem with starting endpoint", e);
		}
	}

	public void stop()
	{
		if (stopping.compareAndSet(false, true)) {
			// resetListeners();
			unregisterTerminal(ciscoTerm);
			unregisterTerminalListeners();
		}
	}

	
    public void invite(String opponentNum, int inviteTimeout, int maxCallDur
            , final IvrEndpointConversationListener listener
            , Map<String, Object> bindings)
    {
        Call call = null;
        try {
            lock.writeLock().lock();
            try {
                if (state.getId()!=IvrTerminalState.IN_SERVICE)
                    throw new Exception("Can't invite oppenent to conversation. Terminal not ready");
                if (calls.size()>=maxChannels)
                    throw new Exception("Can't invite oppenent to conversation. Too many opened channels");
                call = provider.createCall();
                IvrEndpointConversationImpl conv = new IvrEndpointConversationImpl(rtpStreamManager, enableIncomingRtp, address, bindings);
                conv.addConversationListener(listener);
                conv.addConversationListener(this);
                ConvHolder holder = new ConvHolder(conv, false);
                calls.put(call, holder);
                call.connect(ciscoTerm, termAddress, opponentNum);
/*                if (inviteTimeout>0) 
                    executor.execute(inviteTimeout*1000, new InviteTimeoutHandler(conv, call, maxCallDur));
                else if (maxCallDur>0)
                    executor.execute(maxCallDur*1000, new MaxCallDurationHandler(conv, call));*/
            } finally {
                lock.writeLock().unlock();
            }
        } catch (Throwable e) {
            log.warn(String.format("Problem with inviting abonent with number (%s)", opponentNum), e);
            final IvrEndpointConversationStoppedEvent ev = new IvrEndpointConversationStoppedEventImpl(null, CompletionCode.TERMINAL_NOT_READY);
            if (call != null) {
                stopConversation(call, CompletionCode.OPPONENT_UNKNOWN_ERROR);
            }
/*            executor.executeQuietly(new AbstractTask(term, "Propagating conversation stop event") {
                @Override public void doRun() throws Exception {*/
                    listener.conversationStopped(ev);
/*                }
            });*/
            conversationStopped(ev);
        }
    }
	
	// ------------------------- helpers -------------------------
	private CiscoTerminal registerTerminal(Address addr)
			throws Exception
	{
		Terminal[] terminals = addr.getTerminals();
		if (terminals == null || terminals.length == 0) {
			throw new Exception(String.format("Address (%s) does not have terminals", address));
		}

		CiscoTerminal terminal = (CiscoTerminal) terminals[0];
		if (terminal instanceof CiscoRouteTerminal) {
			if (log.isDebugEnabled()) {
				log.debug("Registering terminal (" + CiscoRouteTerminal.class.getName() + ")");
			}

			CiscoRouteTerminal routeTerm = (CiscoRouteTerminal) terminal;
			if (routeTerm.isRegisteredByThisApp()) {
				unexpectedUnregistration(routeTerm);
			}
			routeTerm.register(codec.getCiscoMediaCapabilities(), CiscoRouteTerminal.DYNAMIC_MEDIA_REGISTRATION);
			return routeTerm;
		}
		else if (terminal instanceof CiscoMediaTerminal) {
			if (log.isDebugEnabled()) {
				log.debug("Registering terminal (" + CiscoMediaTerminal.class.getName() + ")");
			}
			CiscoMediaTerminal mediaTerm = (CiscoMediaTerminal) terminal;
			if (mediaTerm.isRegisteredByThisApp()) {
				unexpectedUnregistration(mediaTerm);
			}
			mediaTerm.register(codec.getCiscoMediaCapabilities());
			maxChannels = 1;
			return mediaTerm;
		}
		throw new Exception(String.format("Invalid terminal class. Expected one of: %s, %s. But was %s",
				CiscoRouteTerminal.class.getName(), CiscoMediaTerminal.class.getName(), terminal.getClass().getName()));
	}

	private void registerTerminalListeners()
			throws Exception
	{
		ciscoTerm.addObserver(this);
		termAddress.addObserver(this);
		termAddress.addCallObserver(this);
	}

	private void unregisterTerminal(Terminal term)
	{
		try {
			if (term instanceof CiscoRouteTerminal) {
				((CiscoRouteTerminal) term).unregister();
			}
			else if (term instanceof CiscoMediaTerminal) {
				((CiscoMediaTerminal) term).unregister();
			}
		}
		catch (Throwable e) {
			log.error("Problem with terminal unregistration", e);
		}
	}

	private void unexpectedUnregistration(Terminal term)
	{
		log.warn("Unexpected terminal unregistration. Triyng to register terminal but it " + "already registered by this application! "
				+ "So unregistering terminal first");
		unregisterTerminal(term);
	}

	private void unregisterTerminalListeners()
	{
		try {
			try {
				termAddress.removeCallObserver(this);
			}
			finally {
				try {
					termAddress.removeObserver(this);
				}
				finally {
					ciscoTerm.removeObserver(this);
				}
			}
		}
		catch (Throwable e) {
			log.warn("Problem with unregistering listeners from the cisco terminal", e);
		}
	}

	private String eventsToString(Object[] events)
	{
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < events.length; ++i)
			buf.append(i > 0 ? ", " : "").append(events[i].toString());
		return buf.toString();
	}

	private synchronized void checkState()
	{
		if (state.getId() == IvrTerminalState.OUT_OF_SERVICE && !stopping.get()) {
			if (termInService && termAddressInService)
				state.setState(IvrTerminalState.IN_SERVICE);
		}
		else if (!termAddressInService || !termInService)
			state.setState(IvrTerminalState.OUT_OF_SERVICE);
	}

	private void createConversation(Call call)
	{
		lock.writeLock().lock();
		try {
			try {
				ConvHolder holder = calls.get(call);
				if (holder == null && enableIncomingCalls && calls.size() <= maxChannels) {
					if (log.isDebugEnabled()) {
						log.debug(callLog(call, "Creating conversation"));
					}
					IvrEndpointConversationImpl conv = new IvrEndpointConversationImpl(rtpStreamManager, enableIncomingRtp, address, null);
					conv.setCall((CallControlCall) call);
					conv.addConversationListener(this);
					calls.put(call, new ConvHolder(conv, true));
				}
				else if (holder != null && !holder.incoming)
					holder.conv.setCall((CallControlCall) call);
			}
			catch (Throwable e) {
				log.error("Error creating conversation", e);
			}
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	private void initInRtp(CiscoMediaOpenLogicalChannelEv ev)
	{
		ConvHolder conv = getConvHolderByConnId(ev.getCiscoRTPHandle().getHandle());
		if (conv == null)
			return;
		try {
			if (log.isDebugEnabled()) {
				log.debug("Initializing incoming RTP stream");
			}
			IncomingRtpStream rtp = conv.conv.initIncomingRtp();
			CiscoRTPParams params = new CiscoRTPParams(rtp.getAddress(), rtp.getPort());
			if (ev.getTerminal() instanceof CiscoMediaTerminal)
				((CiscoMediaTerminal) ev.getTerminal()).setRTPParams(ev.getCiscoRTPHandle(), params);
			else if (ev.getTerminal() instanceof CiscoRouteTerminal)
				((CiscoRouteTerminal) ev.getTerminal()).setRTPParams(ev.getCiscoRTPHandle(), params);
		}
		catch (Throwable e) {
			if (conv.conv.getState().getId() != IvrEndpointConversationState.INVALID) {
				log.error("Error initializing incoming RTP stream", e);
			}
			conv.conv.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
		}
	}

	
    private void initAndStartOutRtp(CiscoRTPOutputStartedEv ev) {
        ConvHolder conv = getConvHolderByCall(ev.getCallID().getCall());
        if (conv == null) {
            return;
        }
          
        try {
            CiscoRTPOutputProperties props = ev.getRTPOutputProperties();
            if (log.isDebugEnabled()) {
                    log.debug(callLog(ev.getCallID().getCall(),
                            "Proposed RTP params: remoteHost (%s), remotePort (%s), packetSize (%s ms), " +
                            "payloadType (%s), bitrate (%s)"
                            , props.getRemoteAddress().toString(), props.getRemotePort()
                            , props.getPacketSize(), props.getPayloadType(), props.getBitRate()));
            }
            Integer psize = rtpPacketSize;
            Codec streamCodec = Codec.getCodecByCiscoPayload(props.getPayloadType());
            if (streamCodec==null)
                throw new Exception(String.format(
                        "Not supported payload type (%s)", props.getPayloadType()));
            if (psize==null) {
                psize = (int)streamCodec.getPacketSizeForMilliseconds(props.getPacketSize());
            }
            if (log.isDebugEnabled()) {
                log.debug(callLog(ev.getCallID().getCall()
                    ,"Choosed RTP params: packetSize (%s ms), codec (%s), audioFormat (%s)"
                    , streamCodec.getMillisecondsForPacketSize(psize), streamCodec, streamCodec.getAudioFormat()));
            }
            conv.conv.initOutgoingRtp(props.getRemoteAddress().getHostAddress(), props.getRemotePort()
                    , psize, rtpMaxSendAheadPacketsCount, streamCodec);
            conv.conv.startOutgoingRtp();
        } catch (Throwable e) {
            log.error(callLog(ev.getCallID().getCall() ,"Error initializing and starting outgoing RTP stream"), e);
            conv.conv.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
        }
    }

    private void startInRtp(CiscoRTPInputStartedEv ev) {
        ConvHolder conv = getConvHolderByCall(ev.getCallID().getCall());
        if (conv==null)
            return;
        try {
            conv.conv.startIncomingRtp();
        } catch (Throwable e) {
        	if (log.isDebugEnabled()) {
                log.error(callLog(ev.getCallID().getCall(), "Problem with start incoming RTP stream"), e);
        	}
            conv.conv.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
        }
    }

    private void stopOutRtp(CiscoRTPOutputStoppedEv ev) {
        ConvHolder conv = getConvHolderByCall(ev.getCallID().getCall());
        if (conv!=null)
            conv.conv.stopOutgoingRtp();
    }
    
    private void stopInRtp(CiscoRTPInputStoppedEv ev) {
        ConvHolder conv = getConvHolderByCall(ev.getCallID().getCall());
        if (conv!=null)
            conv.conv.stopIncomingRtp();
    }
	
	
	private ConvHolder getConvHolderByConnId(int connId)
	{
		lock.readLock().lock();
		try {
			return connIds.get(connId);
		}
		finally {
			lock.readLock().unlock();
		}
	}

    private ConvHolder getConvHolderByCall(Call call) {
        lock.readLock().lock();
        try {
            return calls.get(call);
        } finally {
            lock.readLock().unlock();
        }
    }
	
    private ConvHolder getAndRemoveConvHolder(Call call) {
        lock.writeLock().lock();
        try {
            return calls.remove(call);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
	private class ConvHolder
	{
		private final IvrEndpointConversationImpl conv;
		private final boolean incoming;
		private final long created = System.currentTimeMillis();

		public ConvHolder(IvrEndpointConversationImpl conv, boolean incoming)
		{
			this.conv = conv;
			this.incoming = incoming;
		}

		public long getDuration()
		{
			return (System.currentTimeMillis() - created) / 1000;
		}

		@Override
		public String toString()
		{
			return conv.toString();
		}
	}

	private static String callLog(Call call, String message, Object... args)
	{
		return getCallDesc((CiscoCall) call) + " : Terminal. " + String.format(message, args);
	}

	private static String getCallDesc(CiscoCall call)
	{
		return "[call id: " + call.getCallID().intValue() + ", calling number: " + call.getCallingAddress().getName() + "]";
	}

    //--------------- IvrEndpointConversationListener methods -----------------//
    public void listenerAdded(final IvrEndpointConversationEvent event) {
        fireConversationEvent(new MethodCaller() {
            @Override public void callMethod(IvrEndpointConversationListener listener) {
                listener.listenerAdded(event);
            }
        });
    }

    public void conversationStarted(final IvrEndpointConversationEvent event) {
        fireConversationEvent(new MethodCaller() {
            @Override public void callMethod(IvrEndpointConversationListener listener) {
                listener.conversationStarted(event);
            }
        });
    }

    public void conversationStopped(final IvrEndpointConversationStoppedEvent event) {
//        if (event.getCompletionCode()==CompletionCode.COMPLETED_BY_ENDPOINT)
//            getAndRemoveConvHolder(event.getCall());
        fireConversationEvent(new MethodCaller() {
            @Override public void callMethod(IvrEndpointConversationListener listener) {
                listener.conversationStopped(event);
            }
        });
    }

    public void conversationTransfered(final IvrEndpointConversationTransferedEvent event) {
        fireConversationEvent(new MethodCaller() {
            @Override public void callMethod(IvrEndpointConversationListener listener) {
                listener.conversationTransfered(event);
            }
        });
    }

    public void incomingRtpStarted(final IvrIncomingRtpStartedEvent event) {
        fireConversationEvent(new MethodCaller() {
            @Override public void callMethod(IvrEndpointConversationListener listener) {
                listener.incomingRtpStarted(event);
            }
        });
    }

    public void outgoingRtpStarted(final IvrOutgoingRtpStartedEvent event) {
        fireConversationEvent(new MethodCaller() {
            @Override public void callMethod(IvrEndpointConversationListener listener) {
                listener.outgoingRtpStarted(event);
            }
        });
    }

    public void dtmfReceived(final IvrDtmfReceivedConversationEvent event) {
        fireConversationEvent(new MethodCaller() {
            @Override public void callMethod(IvrEndpointConversationListener listener) {
                listener.dtmfReceived(event);
            }
        });
    }

    private void fireConversationEvent(MethodCaller method) {
        listenersLock.readLock().lock();
        try {
            for (IvrEndpointConversationListener listener : conversationListeners)
                method.callMethod(listener);
        } finally {
            listenersLock.readLock().unlock();
        }
    }
    
    private abstract class MethodCaller {
        public abstract void callMethod(IvrEndpointConversationListener listener);
    }
    
    private void bindConnIdToConv(ConnConnectedEv ev) {
        lock.writeLock().lock();
        try {
            if (ev.getConnection().getAddress().getName().equals(address)) {
                ConvHolder conv = calls.get(ev.getCall());
                if (conv!=null) {
                    connIds.put(((CiscoConnection)ev.getConnection()).getConnectionID().intValue(), conv);
                    if (log.isDebugEnabled()) {
                        log.debug(callLog(ev.getCall(), "Connection ID binded to the conversation"));
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void unbindConnIdFromConv(ConnDisconnectedEv ev) {
        if (address.equals(ev.getConnection().getAddress().getName())) {
            lock.writeLock().lock();
            try {
            	if (log.isDebugEnabled()) {
                    log.debug(callLog(ev.getCall(), "Unbinding connection ID from the conversation"));
            	}
                connIds.remove(((CiscoConnection)ev.getConnection()).getConnectionID().intValue());
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private void acceptIncomingCall(CallCtlConnOfferedEv ev) {
        ConvHolder conv = getConvHolderByCall(ev.getCall());
        if (conv==null || !conv.incoming) //|| !conv.incoming
            return;
        try {
        	if (log.isDebugEnabled()) {
                log.debug(callLog(ev.getCall(), "Accepting call"));
        	}
            ((CallControlConnection)ev.getConnection()).accept();
        } catch (Throwable e) {
            log.error(callLog(ev.getCall(), "Problem with accepting call"), e);
        }
    }

    private void answerOnIncomingCall(TermConnRingingEv ev) {
        try {
        	if (log.isDebugEnabled()) {
                log.debug(callLog(ev.getCall(), "Answering on call"));
        	}
            ev.getTerminalConnection().answer();
        } catch (Throwable e) {
            log.error(callLog(ev.getCall(), "Problem with answering on call"), e);
        }
    }
    
    private void openLogicalChannel(CallCtlConnEstablishedEv ev) {
    	if (log.isDebugEnabled()) {
            log.debug(callLog(ev.getCall(), "Logical connection opened for address (%s)"
                    , ev.getConnection().getAddress().getName()));
    	}
        ConvHolder conv = getConvHolderByCall(ev.getCall());
        if (conv!=null)
            try {
                conv.conv.logicalConnectionCreated(ev.getConnection().getAddress().getName());
            } catch (IvrEndpointConversationException e) {
                    log.error(callLog(ev.getCall(), 
                            "Error open logical connection for address (%s)"
                            , ev.getConnection().getAddress().getName()), e);
                conv.conv.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
            }
    }
 
    private void callTransfered(CiscoTransferEndEv ev) {
//      if (isLogLevelEnabled(LogLevel.DEBUG))
//          logger.debug(callLog(ev.getCall(), "Logical connection opened for address (%s)"
//                  , ev.getConnection().getAddress().getName()));
//      System.out.println("!!! Transfer controller address: "+ev.getTransferControllerAddress().getName());
//      ConvHolder conv = getConvHolderByCall(ev.getCall());
//      if (conv!=null) {
//          conv.conv.opponentPartyTransfered();
//      }
//          try {
//          } catch (IvrEndpointConversationException e) {
//              if (isLogLevelEnabled(LogLevel.ERROR))
//                  logger.error(callLog(ev.getCall(), 
//                          "Error open logical connection for address (%s)"
//                          , ev.getConnection().getAddress().getName()), e);
//              conv.conv.stopConversation(CompletionCode.OPPONENT_UNKNOWN_ERROR);
//          }
  }
   
    private void continueConv(MediaTermConnDtmfEv ev) {
        ConvHolder conv = getConvHolderByCall(ev.getCall());
        if (conv!=null)
            conv.conv.continueConversation(ev.getDtmfDigit());
    }

    private void handleConnFailedEvent(CallCtlConnFailedEv ev) {
        ConvHolder conv = getAndRemoveConvHolder(ev.getCall());
        if (conv==null)
            return;
        int cause = ev.getCallControlCause();
        CompletionCode code = CompletionCode.OPPONENT_UNKNOWN_ERROR;
        switch (cause) {
            case CallCtlConnFailedEv.CAUSE_BUSY:
                code = CompletionCode.OPPONENT_BUSY;
                break;
            case CallCtlConnFailedEv.CAUSE_CALL_NOT_ANSWERED:
            case CallCtlConnFailedEv.CAUSE_NORMAL:
                code = CompletionCode.OPPONENT_NOT_ANSWERED;
                break;
        }
        conv.conv.stopConversation(code);
    }
    
    private void stopConversation(Call call, CompletionCode completionCode) {
        ConvHolder conv = getAndRemoveConvHolder(call);
        if (conv!=null) {
            conv.conv.stopConversation(completionCode);
        }
    }
    
}
