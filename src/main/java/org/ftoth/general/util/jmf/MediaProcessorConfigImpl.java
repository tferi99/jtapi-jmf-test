package org.ftoth.general.util.jmf;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.protocol.FileTypeDescriptor;

public class MediaProcessorConfigImpl implements MediaProcessorConfig
{
	// ------------------------- properties -------------------------

	// inputDataUrl : URL for input data source
	// contentType : You can use the Processor setContentDescriptor method to specify the format of the data output by the Processor.
	// Setting the output data format to null causes the media data to be rendered instead of output to
	// the Processor object's output DataSource.
	// customProcessing : to build custom plugin chains
	// desiredOutputFormat : expected output format of tracks
	// presentingTarget : destination action after processing to present processor output

	// ------------------ input ---------------------
	private String inputDataUrl;
	// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/easymoney64.wav";
	// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/3_ulaw.wav";
	// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/Encoded.wav";
	// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/pcm-8000Hz-8b-mono.wav";
	// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/pcm-8000Hz-16b-mono.wav";
	// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/pcm-8000Hz-16b-mono.pcm";
	// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/short.wav";
	// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/nobody.wav";
	// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/nobody_gsm.wav";
	// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/Sample.g729";
	// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/Encoded.g729";
	//private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/out.g729";
	// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/out.pcm";
	// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/x.g729";

	// ------------------ processing ---------------------
	// output content type - you may have to change this if you change presentingTarget
	// e.g. for FILE mode (saving into wav) you need FileTypeDescriptor.WAVE
	// and for RTP you need ContentDescriptor.RAW_RTP
	// WAVE, RAW, RAW_RTP, MIXED, CONTENT_UNKNOWN
	private FileTypeDescriptor contentType = null; // for rendering, same if you choose PresentingTarget.PLAYER
	// private FileTypeDescriptor contentType = new FileTypeDescriptor(FileTypeDescriptor.WAVE);
	//private FileTypeDescriptor contentType = new FileTypeDescriptor(FileTypeDescriptor.RAW_RTP);
	// private FileTypeDescriptor contentType = new FileTypeDescriptor(FileTypeDescriptor.RAW);
	// private FileTypeDescriptor contentType = new FileTypeDescriptor(HeadlessAudioMux.OUTPUT_FORMAT_HEADLESS_G729);
	// private FileTypeDescriptor contentType = new FileTypeDescriptor(HeadlessAudioMux.OUTPUT_FORMAT_HEADLESS_LINEAR);
	// private FileTypeDescriptor contentType = new FileTypeDescriptor("lofasz");

	// ------------------------- custom processing -------------------------
	// custom processing
	// NONE, ULAW_RTP, G729, ....
	private MediaProcessor.CustomProcessing customProcessing = MediaProcessor.CustomProcessing.NONE;
	// private CustomProcessing customProcessing = CustomProcessing.G729;
	// private CustomProcessing customProcessing = CustomProcessing.TEST;
	//private CustomProcessing customProcessing = CustomProcessing.G729_RTP_FROM_RAW;

	// ------------------------- desired output format -------------------------
	// desired output format for processing
	//
	// NOTE: not all output format can be specified for automatic processing,
	// e.g. the following formats cannot work here:
	// GSM, DVI, G723
	//
	// AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, endian, signed, frameSizeInBits, frameRate, dataType)
	// ULAW, ULAW_RTP, G729_RTP, DVI_RTP, GSM_RTP
	//
	private Format desiredOutputFormat;
	// LINEAR
	// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
	// Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray);
	// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.LINEAR, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray);
	// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.LINEAR, 8000, 16, 1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray);
	// ULAW
	// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.ULAW, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
	// Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray);
	// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.ULAW, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 8, Format.NOT_SPECIFIED, Format.byteArray);
	// ULAW_RTP
	// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.ULAW_RTP, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
	// Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray);
	// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.ULAW_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 8, Format.NOT_SPECIFIED,
	// Format.byteArray);
	// G729
	// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.G729, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
	// Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray);
	// private AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.G729, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 8, Format.NOT_SPECIFIED, Format.byteArray);
	// private AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.G729, 8000, 8, 1);
	// G729_RTP
	//private Format desiredOutputFormat = new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 8, Format.NOT_SPECIFIED, Format.byteArray);
	// GSM
	// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.GSM_RTP, 8000, 0, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 520, Format.NOT_SPECIFIED,
	// Format.byteArray);
	// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.GSM_MS, 8000, 0, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 520, Format.NOT_SPECIFIED,
	// Format.byteArray);


	/**
	 * Interactive mode with UI
	 */
	private boolean interactiveMode = false;

	/**
	 * Automatically start in interactive mode (only of interactiveMode is true)
	 */
	private boolean autoStartProcessor = true;

	// ------------------ output, presenting ---------------------
	private MediaProcessor.PresentingTarget presentingTarget;
	//private MediaProcessor.PresentingTarget presentingTarget = MediaProcessor.PresentingTarget.RTP; // sending output of processor to: NONE, SINK, RTP, PLAYER

	//private String rtpTargetAddress = "10.122.188.255";
	private String rtpTargetAddress;
	private int rtpTargetPort;

	private String outputSinkDataUrl;
	// private String outputSinkDataUrl = "file:/c:/Users/ftoth/Documents/media/out.wav";

	@Override
	// private String outputSinkDataUrl = "file:/c:/Users/ftoth/Documents/media/pcm-8000Hz-16b-mono.pcm";
	public String getInputDataUrl()
	{
		return inputDataUrl;
	}

	@Override
	public void setInputDataUrl(String inputDataUrl)
	{
		this.inputDataUrl = inputDataUrl;
	}

	@Override
	public FileTypeDescriptor getOutputContentType()
	{
		return contentType;
	}

	@Override
	public void setOutputContentType(FileTypeDescriptor outputContentType)
	{
		this.contentType = outputContentType;
	}

	@Override
	public MediaProcessor.CustomProcessing getCustomProcessing()
	{
		return customProcessing;
	}

	@Override
	public void setCustomProcessing(MediaProcessor.CustomProcessing customProcessing)
	{
		this.customProcessing = customProcessing;
	}

	@Override
	public Format getDesiredOutputFormat()
	{
		return desiredOutputFormat;
	}

	@Override
	public void setDesiredOutputFormat(Format desiredOutputFormat)
	{
		this.desiredOutputFormat = desiredOutputFormat;
	}

	public MediaProcessor.PresentingTarget getPresentingTarget()
	{
		return presentingTarget;
	}

	public void setPresentingTarget(MediaProcessor.PresentingTarget presentingTarget)
	{
		this.presentingTarget = presentingTarget;
	}

	@Override
	public String getRtpTargetAddress()
	{
		return rtpTargetAddress;
	}

	@Override
	public void setRtpTargetAddress(String rtpTargetAddress)
	{
		this.rtpTargetAddress = rtpTargetAddress;
	}

	@Override
	public int getRtpTargetPort()
	{
		return rtpTargetPort;
	}

	@Override
	public void setRtpTargetPort(int rtpTargetPort)
	{
		this.rtpTargetPort = rtpTargetPort;
	}

	@Override
	public String getOutputSinkDataUrl()
	{
		return outputSinkDataUrl;
	}

	@Override
	public void setOutputSinkDataUrl(String outputSinkDataUrl)
	{
		this.outputSinkDataUrl = outputSinkDataUrl;
	}

	@Override
	public boolean isInteractiveMode()
	{
		return interactiveMode;
	}

	@Override
	public void setInteractiveMode(boolean interactiveMode)
	{
		this.interactiveMode = interactiveMode;
	}

	@Override
	public boolean isAutoStartProcessor()
	{
		return autoStartProcessor;
	}

	@Override
	public void setAutoStartProcessor(boolean autoStartProcessor)
	{
		this.autoStartProcessor = autoStartProcessor;
	}
}
