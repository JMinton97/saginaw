package project.model;

import project.map.MyMap;
import project.utils.ImageFile;
import project.utils.UnsupportedImageTypeException;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Alan P. Sexton
 * Date: 20/06/13
 * Time: 23:36
 */

/**
 * The <code>Model</code> class manages the data of the application
 * <p>
 * Other than possibly a draw method, which draws a representation of the object
 * on a graphics context, and possibly a toString method, which generates a
 * <code>String</code> representation of the object, it should not know about
 * the user interface
 * </p>
 */
public class Model
{
	private BufferedImage	image	= null;
	private MyMap map = null;
	private List<Rectangle>	rects	= new ArrayList<Rectangle>();

	public Model()
	{
	}

	public BufferedImage getImage()
	{
		return image;
	}

	/**
	 * Get the list of rectangles currently set in the model.
	 * 
	 * @return the <code>List</code> object containing the rectangles set
	 */
	public List<Rectangle> getRects()
	{
		return rects;
	}

	/**
	 * Sets or replaces the current image in the <code>Model</code> and clears
	 * the list of rectangles.
	 *
	 * @param bi
	 *            the image to set in the <code>Model</code>
	 */
	public void setImage(BufferedImage bi)
	{
		image = bi;
		rects.clear();
	}

	/**
	 * Get the dimensions of the image loaded
	 * 
	 * @return the <code>Dimension</code> object containing the dimensions of
	 *         the image loaded, or <code>(0, 0)</code> if there is no image
	 *         loaded.
	 */
	public Dimension getDimensions()
	{
		if (image != null)
			return new Dimension(image.getWidth(), image.getHeight());
		else
			return new Dimension(0, 0);
	}

	/**
	 * Adds a new <code>Rectangle</code> to the <code>Model</code>
	 *
	 * @param rect
	 *            the <code>Rectangle</code> to add to the <code>Model</code>
	 */
	public void addRect(Rectangle rect)
	{
		rects.add(rect);
	}

	/**
	 * Tests if the model is active, i.e. whether it currently has an image
	 *
	 * @return <code>true</code> if the model has an image, false otherwise
	 */
	public boolean isActive()
	{
		return image != null;
	}

	/**
	 * Loads an image from a file.
	 * 
	 * Any pre-existing rectangles will be cleared.
	 * 
	 * @param file
	 *            The <code>File</code> object identifying the file containing
	 *            the image to load
	 * @throws IOException
	 *             if there is a problem reading the file or if the file
	 *             contains no images
	 * @throws UnsupportedImageTypeException
	 *             if the file does not contain an image of a type supported
	 */
	public void loadImage(File file)
			throws IOException, UnsupportedImageTypeException
	{
		ImageFile newImageFile = new ImageFile(file);
		int numImages = newImageFile.getNumImages();
		if (numImages == 0)
			throw new IOException("Image file contains no images");
		BufferedImage bi = newImageFile.getBufferedImage(0);
		setImage(bi);
	}

	public void loadMap(File file)
			throws IOException
	{
		this.map = null;
		System.out.println("null");
		System.out.println(Long.toString(file.length()));
		this.map = new MyMap(file); //note - loading file twice
//		int numImages = newImageFile.getNumImages();
//		if (numImages == 0)
//			throw new IOException("Image file contains no images");
		map.drawMap(0);
		BufferedImage bi = map.getMap();
		setImage(bi);
	}

}
