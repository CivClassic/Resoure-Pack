package com.github.maxopoly;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;

public class MakeCompactedOverlay {

	private static final String inputPath = "/home/max/Desktop/Faithful-1.16.1_SF/vanilla";
	private static final String outputPath = "/home/max/eclipse/cpp-photon/java/civclassic-resourcepack/16x";
	private static final String overlayImageSmall = "/home/max/eclipse/cpp-photon/java/civclassic-resourcepack/cross_16_small.png";
	private static final String overlayImageBig = "/home/max/eclipse/cpp-photon/java/civclassic-resourcepack/cross_16_big.png";

	public static void main(String[] args) {
		File sourceFolder = new File(inputPath);
		checkFolderExists(sourceFolder);
		File outputFolder = new File(outputPath);
		checkFolderExists(outputFolder);
		File sourceAssets = discoverFolder(sourceFolder, "assets");
		File sourceMinecraft = discoverFolder(sourceAssets, "minecraft");
		File sourceTextures = discoverFolder(sourceMinecraft, "textures");
		File sourceItems = discoverFolder(sourceTextures, "item");
		File sourceBlocks = discoverFolder(sourceTextures, "block");
		File citTarget = new File(outputPath, "assets/minecraft/optifine/cit");
		File textureTarget = new File(outputPath, "assets/minecraft/textures/block/civclassic");
		textureTarget.mkdirs();
		citTarget.mkdirs();
		File targetItems = new File(citTarget, "compacted_items");
		File targetBlocks = new File(citTarget, "compacted_blocks");
		targetItems.mkdirs();
		targetBlocks.mkdirs();
		convertItems(sourceItems, targetItems);
		convertBlocks(sourceBlocks, targetBlocks, textureTarget);
	}

	private static void convertItems(File itemFolder, File targetFolder) {
		BufferedImage itemOverlay = loadImage(new File(overlayImageSmall));
		for (File file : itemFolder.listFiles()) {
			if (!(file.getName().endsWith(".png") && file.isFile())) {
				System.out.println("Ignoring invalid file " + file.getName());
				continue;
			}
			BufferedImage itemImage = loadImage(file);
			standardItemOverlay(file, itemImage, itemOverlay, targetFolder);
		}
	}

	private static void convertBlocks(File blockFolder, File optifineTargetFolder, File blockTextureTargetFolder) {
		BufferedImage smallOverlay = loadImage(new File(overlayImageSmall));
		BufferedImage bigOverlay = loadImage(new File(overlayImageBig));
		for (File file : blockFolder.listFiles()) {
			if (!(file.getName().endsWith(".png") && file.isFile())) {
				System.out.println("Ignoring invalid file " + file.getName());
				continue;
			}
			BufferedImage itemImage = loadImage(file);
			if (hasEmptySpot(itemImage)) {
				standardItemOverlay(file, itemImage, smallOverlay, optifineTargetFolder);
				continue;
			}
			stitchImages(itemImage, bigOverlay);
			saveImage(new File(blockTextureTargetFolder, "compacted_" + file.getName()), itemImage);
			String normalName = getFileNameWithoutEnding(file);
			writeFile(new File(optifineTargetFolder, "compacted_" + normalName + ".properties"),
					String.format("type=item%nmatchItems=%s%nmodel=%s%nnbt.display.Lore.*=Compacted Item%n", normalName,
							"compacted_" + normalName));
			writeFile(new File(optifineTargetFolder, "compacted_" + normalName + ".json"), String.format(
					"{\"parent\": \"minecraft:block/%s\",\"textures\": {\"up\": \"minecraft:block/civclassic/compacted_%s\"}}",
					normalName, normalName));
		}
	}

	private static void standardItemOverlay(File file, BufferedImage itemImage, BufferedImage itemOverlay,
			File targetFolder) {
		stitchImages(itemImage, itemOverlay);
		saveImage(new File(targetFolder, "compacted_" + file.getName()), itemImage);
		String normalName = getFileNameWithoutEnding(file);
		writeFile(new File(targetFolder, "compacted_" + normalName + ".properties"),
				String.format("type=item%nmatchItems=%s%ntexture=compacted_%s%nnbt.display.Lore.*=Compacted Item%n",
						normalName, normalName));
	}

	private static String getFileNameWithoutEnding(File file) {
		return file.getName().substring(0, file.getName().lastIndexOf('.'));
	}

	private static void writeFile(File file, String content) {
		try (FileWriter fileWriter = new FileWriter(file)) {
			fileWriter.write(content);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean hasEmptySpot(BufferedImage img) {
		for (int w = 0; w < img.getWidth(); w++) {
			for (int h = 0; h < img.getHeight(); h++) {
				int overlaySpot = img.getRGB(w, h);
				if ((overlaySpot & 0xFF000000) == 0) {
					return true;
				}
			}
		}
		return false;
	}

	private static void stitchImages(BufferedImage base, BufferedImage toAdd) {
		for (int w = 0; w < base.getWidth() && w < toAdd.getWidth(); w++) {
			for (int h = 0; h < base.getHeight() && h < toAdd.getHeight(); h++) {
				int overlaySpot = toAdd.getRGB(w, h);
				if ((overlaySpot & 0xFF000000) != 0) {
					// has alpha != 0
					base.setRGB(w, h, overlaySpot);
				}
			}
		}
	}

	private static BufferedImage loadImage(File file) {
		if (!file.isFile()) {
			throw new IllegalStateException("Tried to load image " + file.getAbsolutePath() + ", but it did not exist");
		}
		try {
			return ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException("Failed to load image " + file.getAbsolutePath());
		}
	}

	private static void saveImage(File file, BufferedImage img) {
		try {
			ImageIO.write(img, "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void checkFolderExists(File folder) {
		if (!folder.isDirectory()) {
			throw new IllegalStateException("The folder " + folder.getAbsolutePath() + " did not exist");
		}
	}

	private static File discoverFolder(File parent, String name) {
		File file = new File(parent, name);
		if (!file.isDirectory()) {
			throw new IllegalArgumentException(
					"The folder " + name + " at " + parent.getAbsolutePath() + " could not be found");
		}
		return file;
	}

}
