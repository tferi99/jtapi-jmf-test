package org.ftoth.general.util.onesec.ivr.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.media.Codec;
import javax.media.Demultiplexer;
import javax.media.Format;
import javax.media.Multiplexer;
import javax.media.PlugInManager;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.onesec.ivr.CodecConfig;
import org.ftoth.general.util.onesec.ivr.CodecManager;
import org.ftoth.general.util.onesec.ivr.CodecManagerException;
import org.springframework.stereotype.Component;

@Component("codecManager")
public class CodecManagerImpl implements CodecManager
{
	private static final Log log = LogFactory.getLog(CodecManagerImpl.class);

	private Format alawRtpFormat;
	private Format g729RtpFormat;
	private Map<Format/* inFormat */, Map<Format/* outFormat */, CodecConfigMeta[]>> cache = new HashMap<Format, Map<Format, CodecConfigMeta[]>>();
	private Map<String, Class> parsers = new HashMap<String, Class>();
	private Map<String, Class> coders = new HashMap<String, Class>();
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	private FormatInfo[] formats;

	public CodecManagerImpl() throws IOException
	{
/*		if (PlugInManager.removePlugIn(Packetizer.class.getName(), PlugInManager.CODEC)) {
			if (log.isDebugEnabled()) {
				log.debug("ULAW packetizier codec (with getControls() bug) ({}) successfully removed");
			}
		}
		UlawPacketizer up = new UlawPacketizer();
		PlugInManager.addPlugIn(UlawPacketizer.class.getName(), up.getSupportedInputFormats(), up.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("New ULAW packetizier codec (" + UlawPacketizer.class.getName() + ") successfully added");
		}

		AlawEncoder en = new AlawEncoder();
		PlugInManager.addPlugIn(AlawEncoder.class.getName(), en.getSupportedInputFormats(), en.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("ALAW codec (" + AlawEncoder.class.getName() + ") successfully added");
		}

		AlawPacketizer p = new AlawPacketizer();
		PlugInManager.addPlugIn(AlawPacketizer.class.getName(), p.getSupportedInputFormats(), p.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("ALAW packetizer codec (" + AlawPacketizer.class.getName() + ") successfully added");
		}*/

		////////////////alawRtpFormat = p.getSupportedOutputFormats(null)[0];
		//g729RtpFormat = g.getSupportedOutputFormats(null)[0];

		try {
			PlugInManager.commit();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		getFormatInfo();
		getDemultiplexorsInfo();
		getMultiplexorsInfo();
	}

	private void getFormatInfo()
	{
		ArrayList<FormatInfo> _formats = new ArrayList<FormatInfo>(512);
		Collection<String> codecs = PlugInManager.getPlugInList(null, null, PlugInManager.CODEC);
		for (String className : codecs) {
			try {
				Codec codec = (Codec) Class.forName(className).newInstance();
				Format[] inFormats = codec.getSupportedInputFormats();
				if (inFormats == null || inFormats.length == 0 || !(inFormats[0] instanceof AudioFormat))
					continue;
				for (Format inFormat : inFormats)
					_formats.add(new FormatInfo((AudioFormat) inFormat, codec));
			}
			catch (Exception e) {
				log.error("Error creating instance of codec (" + className + ")");
			}
		}
		formats = new FormatInfo[_formats.size()];
		_formats.toArray(formats);
	}

	private void getDemultiplexorsInfo()
	{
		Collection<String> demuxs = PlugInManager.getPlugInList(null, null, PlugInManager.DEMULTIPLEXER);
		for (String className : demuxs) {
			try {
				Demultiplexer demux = (Demultiplexer) Class.forName(className).newInstance();
				ContentDescriptor[] descs = demux.getSupportedInputContentDescriptors();
				if (descs != null)
					for (ContentDescriptor desc : descs)
						parsers.put(desc.getContentType(), demux.getClass());
			}
			catch (Exception ex) {
				log.error("Error creating instance of demultiplexor (" + className + ")");
			}
		}
	}

	private void getMultiplexorsInfo()
	{
		Collection<String> muxs = PlugInManager.getPlugInList(null, null, PlugInManager.MULTIPLEXER);
		for (String className : muxs) {
			try {
				Multiplexer demux = (Multiplexer) Class.forName(className).newInstance();
				ContentDescriptor[] descs = demux.getSupportedOutputContentDescriptors(null);
				if (descs != null)
					for (ContentDescriptor desc : descs)
						coders.put(desc.getContentType(), demux.getClass());
			}
			catch (Exception ex) {
				log.error("Error creating instance of multiplexor (" + className + ")");
			}
		}
	}

	public Format getAlawRtpFormat()
	{
		return alawRtpFormat;
	}

	public Format getG729RtpFormat()
	{
		return g729RtpFormat;
	}

	public Demultiplexer buildDemultiplexer(String contentType)
	{
		Class parserClass = parsers.get(contentType);
		try {
			return parserClass == null ? null : (Demultiplexer) parserClass.newInstance();
		}
		catch (Exception ex) {
			log.error("Error creating instance of demultiplexor class", ex);
			return null;
		}
	}

	public Multiplexer buildMultiplexer(String contentType)
	{
		Class parserClass = coders.get(contentType);
		try {
			return parserClass == null ? null : (Multiplexer) parserClass.newInstance();
		}
		catch (Exception ex) {
			log.error("Error creating instance of multiplexer class", ex);
			return null;
		}
	}

	public CodecConfig[] buildCodecChain(AudioFormat inFormat, AudioFormat outFormat)
			throws CodecManagerException
	{
		try {
			// getting cached cjhain
			CodecConfig[] codecs = getChainFromCache(inFormat, outFormat);
			if (codecs != null) {
				return codecs;
			}
			
			TailHolder tailHolder = new TailHolder();
			getChainTail(null, inFormat, outFormat, tailHolder);
			checkTail(tailHolder.tail, inFormat, outFormat);
			if (tailHolder.tail == null)
				throw new CodecManagerException(String.format("Can't find codec chain to convert data from (%s) to (%s)", inFormat, outFormat));
			// creating result and caching chain
			CodecNode tail = tailHolder.tail;
			CodecConfigMeta[] meta = new CodecConfigMeta[tail.level];
			codecs = new CodecConfig[meta.length];
			int i = meta.length - 1;
			while (tail != null) {
				meta[i] = new CodecConfigMeta(tail.codec, tail.inFormat, tail.outFormat);
				codecs[i] = meta[i].createCodecConfig();
				tail = tail.parent;
				i--;
			}
			cacheChain(inFormat, outFormat, meta);
			return codecs;
		}
		catch (Exception ex) {
			throw new CodecManagerException("Error creating codec chain", ex);
		}
	}

	private void checkTail(CodecNode tail, AudioFormat f1, AudioFormat f2)
			throws CodecManagerException
	{
		if (tail == null)
			throw new CodecManagerException(String.format("Can't find codec chain to convert data from (%s) to (%s)", f1, f2));
	}

	private CodecConfig[] getChainFromCache(Format inFormat, Format outFormat)
			throws Exception
	{
		CodecConfigMeta[] meta = null;
		lock.readLock().lock();
		try {
			Map<Format, CodecConfigMeta[]> m = cache.get(inFormat);
			if (m != null)
				meta = m.get(outFormat);
		}
		finally {
			lock.readLock().unlock();
		}
		if (meta == null)
			return null;
		CodecConfig[] res = new CodecConfig[meta.length];
		for (int i = 0; i < meta.length; ++i)
			res[i] = meta[i].createCodecConfig();
		return res;
	}

	private void cacheChain(Format inFormat, Format outFormat, CodecConfigMeta[] chainConfig)
	{
		if (lock.writeLock().tryLock()) {
			try {
				Map<Format, CodecConfigMeta[]> ref = cache.get(inFormat);
				if (ref == null) {
					ref = new HashMap<Format, CodecConfigMeta[]>();
					cache.put(inFormat, ref);
				}
				ref.put(outFormat, chainConfig);
			}
			finally {
				lock.writeLock().unlock();
			}
		}
		else {
			log.warn("Can't cache chain configuration because of timeout on cache write lock.");
		}

	}

	private void getChainTail(CodecNode parent, AudioFormat inFormat, AudioFormat outFormat, TailHolder tailHolder)
	{
		if (tailHolder.continueSearch(parent))
			for (FormatInfo format : formats)
				if (format.inFormat.matches(inFormat) && !isChainContainsCodec(parent, format)) {
					Format[] outFormats = format.codec.getSupportedOutputFormats(inFormat);
					if (outFormats != null && outFormats.length > 0)
						for (Format outf : outFormats) {
							CodecNode node = new CodecNode(parent, format.codec.getClass(), inFormat, outf);
							if (outf.matches(outFormat)) {
								tailHolder.setTail(node);
								return;
							}
							else
								getChainTail(node, (AudioFormat) outf, outFormat, tailHolder);
						}
				}
	}

	private boolean isFormatEquals(AudioFormat f1, AudioFormat f2)
	{
		return f1.getEncoding().equals(f2.getEncoding()) && f1.getSampleRate() == f2.getSampleRate() && f1.getChannels() == f2.getChannels() && f1.getEndian() == f2.getEndian()
				&& f1.getSampleSizeInBits() == f2.getSampleSizeInBits() && f1.getSigned() == f2.getSigned();
	}

	private boolean isChainContainsCodec(CodecNode tail, FormatInfo fmt)
	{
		while (tail != null)
			if (tail.codec.equals(fmt.codec.getClass()) || tail.inFormat.matches(fmt.inFormat))
				return true;
			else
				tail = tail.parent;
		return false;
	}

	private final class CodecNode
	{
		private CodecNode parent;
		private final Class codec;
		private AudioFormat inFormat;
		private final AudioFormat outFormat;
		private int level = 1;

		public CodecNode(CodecNode parent, Class codec, Format inFormat, Format outFormat)
		{
			this.parent = parent;
			this.codec = codec;
			this.outFormat = (AudioFormat) outFormat;
			AudioFormat in = (AudioFormat) inFormat, out = this.outFormat;
			this.inFormat = (AudioFormat) inFormat;
			if (parent != null)
				level = parent.level + 1;
		}

		public CodecNode getRoot()
		{
			return parent == null ? this : parent.getRoot();
		}
	}

	private final class CodecConfigMeta
	{
		private final Class codecClass;
		private final Format inputFormat;
		private final Format outputFormat;

		public CodecConfigMeta(Class codecClass, Format inputFormat, Format outputFormat)
		{
			this.codecClass = codecClass;
			this.inputFormat = inputFormat;
			this.outputFormat = outputFormat;
		}

		public CodecConfig createCodecConfig()
				throws Exception
		{
			return new CodecConfigImpl((Codec) codecClass.newInstance(), outputFormat, inputFormat);
		}
	}

	private final class FormatInfo
	{
		private final AudioFormat inFormat;
		private final Codec codec;

		public FormatInfo(AudioFormat inFormat, Codec codec)
		{
			this.inFormat = inFormat;
			this.codec = codec;
		}
	}

	private final class TailHolder
	{
		private CodecNode tail;

		private void setTail(CodecNode node)
		{
			if (tail == null || tail.level > node.level)
				tail = node;
		}

		private boolean continueSearch(CodecNode parent)
		{
			return tail == null || tail.level > parent.level;
		}
	}
}