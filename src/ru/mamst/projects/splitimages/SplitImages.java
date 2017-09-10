package ru.mamst.projects.splitimages;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import javax.imageio.ImageIO;

public class SplitImages {

	private static final String pointsFile = ".txt";
	private static final String jpgFile = ".jpg";
	private static final String pngFile = ".png";

	private static final String pointsSeparator = ",";
	private static final int countPoints = 4;

	private static final String folgerWithImg = "images";
	private static final String folgerWithAnnotations = "annotations";
	private static final String folgerForResult = "fragments";
	private static final String folgerForGreyResult = folgerForResult + "_greyscale";
	private static final String folgerForFlipResult = folgerForResult + "_flip";
	private static final String folgerForNormalizationResult = folgerForResult + "_normalization";
	private static final String folgerForNoiseResult = folgerForResult + "_noise";
	private static final String fileNameGray = "_grey";
	private static final String fileNameFlip = "_flip";
	private static final String fileNameNormalization = "_normalization";
	private static final String fileNameNoise = "_noise";

	private static final float maxBrightness = 255f;
	private static final float minBrightness = 0f;

	public static void main(String[] args) throws URISyntaxException {
		final String projectPath = new File(
				SplitImages.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile()
						.getParentFile().getPath();
		final File imgDir = new File(projectPath + File.separator + folgerWithImg);
		final File annotationDir = new File(projectPath + File.separator + folgerWithAnnotations);
		if (imgDir.exists() && annotationDir.exists() && imgDir.list().length > 0 && annotationDir.list().length > 0) {

			final File dirResultFile = getDir(projectPath + File.separator + folgerForResult);
			final File dirGrayResultFile = getDir(projectPath + File.separator + folgerForGreyResult);
			final File dirFlipResultFile = getDir(projectPath + File.separator + folgerForFlipResult);
			final File dirNormalizationResultFile = getDir(projectPath + File.separator + folgerForNormalizationResult);
			final File dirNoiseResultFile = getDir(projectPath + File.separator + folgerForNoiseResult);

			for (final String imgName : imgDir.list()) {
				final String nameWithoutType;
				final String type;
				if (imgName.endsWith(jpgFile)) {
					nameWithoutType = imgName.replace(jpgFile, "");
					type = jpgFile;
				} else if (imgName.endsWith(pngFile)) {
					nameWithoutType = imgName.replace(pngFile, "");
					type = pngFile;
				} else {
					System.out.println("file " + imgName + " not image type");
					continue;
				}
				final File annotationFile = new File(annotationDir + File.separator + nameWithoutType + pointsFile);
				if (!annotationFile.exists()) {
					System.out.println("not annotation file for image: " + imgName);
					continue;
				}

				final File imgFile = new File(imgDir + File.separator + imgName);
				try (final FileInputStream fisImg = new FileInputStream(imgFile);
						final Scanner scannerTxt = new Scanner(annotationFile)) {

					final BufferedImage buffImg = ImageIO.read(fisImg);
					int i = -1;

					while (scannerTxt.hasNextLine()) {
						i++;
						final String[] points = scannerTxt.nextLine().split(pointsSeparator);
						if (points.length != countPoints) {
							System.out.println("error in count points" + Arrays.toString(points));
							continue;
						}
						final int fragmentHeight = Integer.parseInt(points[3]) - Integer.parseInt(points[1]);
						final int fragmentWidht = Integer.parseInt(points[2]) - Integer.parseInt(points[0]);
						final BufferedImage buffFragment = buffImg.getSubimage(Integer.parseInt(points[0]),
								Integer.parseInt(points[1]), fragmentWidht, fragmentHeight);

						final String fragmentNameWithoutType = nameWithoutType + "_" + i;
						createImgFile(buffFragment,
								dirResultFile.getAbsolutePath() + File.separator + fragmentNameWithoutType + type,
								type.replace(".", ""));

						final BufferedImage buffFragmentGray = getGrayStyle(buffFragment);
						createImgFile(buffFragmentGray, dirGrayResultFile.getAbsolutePath() + File.separator
								+ fragmentNameWithoutType + fileNameGray + type, type.replace(".", ""));

						final BufferedImage buffFrgmentFlip = getFlip(buffFragment);
						createImgFile(buffFrgmentFlip, dirFlipResultFile.getAbsolutePath() + File.separator
								+ fragmentNameWithoutType + fileNameFlip + type, type.replace(".", ""));

						final BufferedImage buffFragmentNorm = getNormalization(buffFrgmentFlip);
						createImgFile(
								buffFragmentNorm, dirNormalizationResultFile.getAbsolutePath() + File.separator
										+ fragmentNameWithoutType + fileNameNormalization + type,
								type.replace(".", ""));

						final BufferedImage buffFragmentNoise = gaussianNoise(buffFragmentGray);
						createImgFile(buffFragmentNoise, dirNoiseResultFile.getAbsolutePath() + File.separator
								+ fragmentNameWithoutType + fileNameNoise + type, type.replace(".", ""));

					}
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
				}
			}
		} else {
			System.out.println("error: directory is empty or not exist");
		}
		System.out.println("end!");
	}

	private static File getDir(final String dirString) {
		final File dir = new File(dirString);
		if (!dir.exists()) {
			dir.mkdir();
		}
		return dir;
	}

	private static void createImgFile(final BufferedImage bufferedImage, final String dir, final String type)
			throws Exception {
		final File file = new File(dir);
		if (file.exists()) {
			file.delete();
		}
		file.createNewFile();
		ImageIO.write(bufferedImage, type, file);
	}

	private static float[][] getMinAndMax(final BufferedImage bufferedImage) {
		float[][] minAndMax = new float[][] { { maxBrightness, maxBrightness, maxBrightness },
				{ minBrightness, minBrightness, minBrightness } };
		for (int x = 0; x < bufferedImage.getWidth(); x++) {
			for (int y = 0; y < bufferedImage.getHeight(); y++) {
				float[] rgb = getRGB(bufferedImage.getRGB(x, y));
				float r = rgb[0];
				float g = rgb[1];
				float b = rgb[2];

				if (minAndMax[0][0] > r) {
					minAndMax[0][0] = r;
				}
				if (minAndMax[0][1] > g) {
					minAndMax[0][1] = g;
				}
				if (minAndMax[0][2] > b) {
					minAndMax[0][2] = b;
				}
				if (minAndMax[1][0] < r) {
					minAndMax[1][0] = r;
				}
				if (minAndMax[1][1] < g) {
					minAndMax[1][1] = g;
				}
				if (minAndMax[1][2] < b) {
					minAndMax[1][2] = b;
				}
			}
		}
		return minAndMax;
	}

	private static BufferedImage getNormalization(final BufferedImage image) {
		float[][] minAndMax = getMinAndMax(image);
		BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				float[] rgb = getRGB(image.getRGB(x, y));
				final Color colorResult = new Color(
						Math.round((rgb[0] - minAndMax[0][0]) * (maxBrightness / minAndMax[1][0])),
						Math.round((rgb[1] - minAndMax[0][1]) * (maxBrightness / minAndMax[1][1])),
						Math.round((rgb[2] - minAndMax[0][2]) * (maxBrightness / minAndMax[1][2])));
				result.setRGB(x, y, colorResult.getRGB());
			}
		}
		return result;
	}

	private static float[] getRGB(final int index) {
		return new float[] { (index & 0x00ff0000) >> 16, (index & 0x0000ff00) >> 8, index & 0x000000ff };
	}

	private static BufferedImage gaussianNoise(final BufferedImage image) {
		Raster source = image.getRaster();
		BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
		WritableRaster out = result.getRaster();

		int currVal;
		double newVal;
		double gaussian;
		int bands = out.getNumBands();
		int width = image.getWidth();
		int height = image.getHeight();
		Random randGen = new Random();

		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				gaussian = randGen.nextGaussian();

				for (int b = 0; b < bands; b++) {
					newVal = 10.0 * gaussian;
					currVal = source.getSample(i, j, b);
					newVal = newVal + currVal;
					if (newVal < 0)
						newVal = 0.0;
					if (newVal > 255)
						newVal = 255.0;

					out.setSample(i, j, b, (int) (newVal));
				}
			}
		}

		return result;
	}

	private static BufferedImage getGrayStyle(final BufferedImage image) {
		final BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(),
				BufferedImage.TYPE_BYTE_GRAY);
		final Graphics graphics = result.getGraphics();
		graphics.drawImage(image, 0, 0, null);
		graphics.dispose();
		return result;
	}

	private static BufferedImage getFlip(final BufferedImage image) {
		final AffineTransform at = new AffineTransform();
		at.concatenate(AffineTransform.getScaleInstance(1, -1));
		at.concatenate(AffineTransform.getTranslateInstance(0, -image.getHeight()));
		final BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics2d = result.createGraphics();
		graphics2d.transform(at);
		graphics2d.drawImage(image, 0, 0, null);
		graphics2d.dispose();
		return result;
	}
}
