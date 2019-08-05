package org.ftoth.general.util.jmf;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.protocol.FileTypeDescriptor;

/**
 * Usage:
 * 	1. choose input data	-> getInputDataUrl()
 * 	2. specify content type of output -> setOutputContentType()
 * 	3. set desired output format -> setDesiredOutputFormat()
 */
public interface MediaProcessorConfig
{
	// ------------------- input -------------------
	String getInputDataUrl();
	void setInputDataUrl(String inputDataUrl);

	// ------------------- output -------------------
	Format getDesiredOutputFormat();
	void setDesiredOutputFormat(Format desiredOutputFormat);

	FileTypeDescriptor getOutputContentType();
	void setOutputContentType(FileTypeDescriptor outputContentType);

	String getRtpTargetAddress();
	void setRtpTargetAddress(String rtpTargetAddress);

	int getRtpTargetPort();
	void setRtpTargetPort(int rtpTargetPort);

	String getOutputSinkDataUrl();
	void setOutputSinkDataUrl(String outputSinkDataUrl);

	MediaProcessor.CustomProcessing getCustomProcessing();
	void setCustomProcessing(MediaProcessor.CustomProcessing customProcessing);

	MediaProcessor.PresentingTarget getPresentingTarget();
	void setPresentingTarget(MediaProcessor.PresentingTarget presentingTarget);

	// ------------------- etc -------------------
	boolean isInteractiveMode();
	void setInteractiveMode(boolean interactiveMode);

	boolean isAutoStartProcessor();
	void setAutoStartProcessor(boolean autoStartProcessor);
}
