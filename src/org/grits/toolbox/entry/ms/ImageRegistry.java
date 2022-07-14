/**
 * 
 */
package org.grits.toolbox.entry.ms;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;

public class ImageRegistry
{
	private static Logger logger = Logger.getLogger(ImageRegistry.class);
	private static final String IMAGE_PATH = "icons" + File.separator;
	private static Map<MSImage, ImageDescriptor> imageCache = new HashMap<MSImage, ImageDescriptor>();

	public static ImageDescriptor getImageDescriptor(String pluginId, MSImage image)
	{
		logger.info("Get image from ms plugin : " + image);

		ImageDescriptor imageDescriptor = null;
		if(image != null)
		{
			imageDescriptor = imageCache.get(image);
			if(imageDescriptor == null)
			{
				logger.info("ImageDescriptor not found in cache");
				URL fullPathString = FileLocator.find(
						Platform.getBundle(pluginId), new Path(IMAGE_PATH + image.iconName), null);

				logger.info("Loading image from url : " + fullPathString);
				if(fullPathString != null)
				{
					imageDescriptor = ImageDescriptor.createFromURL(fullPathString);
					imageCache.put(image, imageDescriptor);
				}
			}
		}
		else
			logger.error("Cannot load image from ms plugin (image name is null)");

		return imageDescriptor;
	}


	/**
	 ***********************************
	 *			Icons
	 ***********************************
	 */
	public enum MSImage
	{
		MASSSPEC_ICON("massspectrum.png"),
		MSANNOTATION_ICON("IconAnnotation.png"),
		MSMERGE_ICON("merge.png"),
		MSCONVERT_ICON("MSConvert.png"),
		GLYCRESOFT_ICON("glycresoft.png"),
		// Masaaki added (7/26/2019)
		PICKER_ICON("SpectraPicker.png"),
		AVERAGE_ICON("SpectraAverage.png");
		
		private String iconName = null;
		private MSImage(String iconName)
		{
			this.iconName  = iconName;
		}
	}


}
