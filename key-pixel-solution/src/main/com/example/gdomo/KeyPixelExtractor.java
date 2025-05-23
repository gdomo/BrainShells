package com.example.gdomo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class KeyPixelExtractor {
    private static final int[] CARD_OFFSETS_X = new int[]{0, 72, 143, 215, 287};

    private static final Rectangle VALUE_AREA = new Rectangle(148, 590, 30, 25);
    private static final Rectangle SUIT_AREA = new Rectangle(170, 633, 30, 35);

    private static final int BLUE_THRESHOLD = 120;

    record PixelsSet(
            Set<Integer> mandatoryPixels,
            Set<Integer> probablePixels
    ) {
    }

    public static void main(String[] args) {
        final File inputFolder = new File(args[0]);

        final Map<Character, List<BufferedImage>> suitToImages = new HashMap<>();
        final Map<String, List<BufferedImage>> valueToImages = new HashMap<>();
        populateAreaSubimages(inputFolder, suitToImages, valueToImages);

        final HashMap<Character, PixelsSet> suitToPixels = getValuablePixels(suitToImages);
        final HashMap<String, PixelsSet> valueToPixels = getValuablePixels(valueToImages);

        findAndPrintKeyPixels(suitToPixels, true, 30);
        System.out.println();
        findAndPrintKeyPixels(valueToPixels, true, 30);
    }

    private static void populateAreaSubimages(File inputFolder, Map<Character, List<BufferedImage>> suitToImages, Map<String, List<BufferedImage>> valueToImages) {
        Arrays.stream(Optional.ofNullable(inputFolder.listFiles()).orElse(new File[0])).forEach(file -> {
            final BufferedImage image;
            try {
                image = ImageIO.read(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String cards = file.getName().substring(0, file.getName().length() - ".png".length());
            int cardNum = 0;
            while (!cards.isEmpty()) {
                final String value = cards.startsWith("10") ? "10" : cards.substring(0, 1);
                cards = cards.substring(value.length());

                final char suit = cards.charAt(0);
                cards = cards.substring(1);

                BufferedImage suitSubimage = image.getSubimage(CARD_OFFSETS_X[cardNum] + SUIT_AREA.x, SUIT_AREA.y, SUIT_AREA.width, SUIT_AREA.height);
                BufferedImage valueSubimage = image.getSubimage(CARD_OFFSETS_X[cardNum] + VALUE_AREA.x, VALUE_AREA.y, VALUE_AREA.width, VALUE_AREA.height);

                suitToImages.computeIfAbsent(suit, v -> new ArrayList<>()).add(suitSubimage);
                valueToImages.computeIfAbsent(value, v -> new ArrayList<>()).add(valueSubimage);

                cardNum++;
            }
        });
    }

    private static <T> HashMap<T, PixelsSet> getValuablePixels(Map<T, List<BufferedImage>> groupToImages) {
        final HashMap<T, PixelsSet> groupToPixels = new HashMap<>();
        for (Map.Entry<T, List<BufferedImage>> entry : groupToImages.entrySet()) {
            final T group = entry.getKey();
            final List<BufferedImage> images = entry.getValue();

            Set<Integer> mandatoryPixels = null;
            Set<Integer> probablePixels = null;

            for (BufferedImage image : images) {
                final int[] areaRgbs = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());

                final HashSet<Integer> currentCardPixels = new HashSet<>();
                for (int i = 0; i < areaRgbs.length; i++) {
                    if (new Color(areaRgbs[i]).getBlue() < BLUE_THRESHOLD) {
                        currentCardPixels.add(i);
                    }
                }

                if (mandatoryPixels == null) {
                    mandatoryPixels = currentCardPixels;
                    probablePixels = currentCardPixels;
                } else {
                    final HashSet<Integer> intersection = new HashSet<>();
                    for (Integer mandatoryPixel : mandatoryPixels) {
                        if (currentCardPixels.contains(mandatoryPixel)) {
                            intersection.add(mandatoryPixel);
                        }
                    }

                    mandatoryPixels = intersection;
                    probablePixels.addAll(currentCardPixels);
                }
            }

            groupToPixels.put(group, new PixelsSet(mandatoryPixels, probablePixels));
        }
        return groupToPixels;
    }

    private static <T> void findAndPrintKeyPixels(HashMap<T, PixelsSet> groups, boolean tryConsequentFirst, int scanSize) {
        final HashMap<T, Collection<Integer>> groupToUniquePixels = new HashMap<>();
        groups.forEach(((group, pixels) -> {
            final SortedSet<Integer> uniquePixels = new TreeSet<>(pixels.mandatoryPixels);

            final HashMap<T, PixelsSet> otherGroups = new HashMap<>(groups);
            otherGroups.remove(group);

            otherGroups.values().forEach(otherGroup -> uniquePixels.removeAll(otherGroup.probablePixels));

            final Collection<Integer> pixelsList = tryConsequentFirst ? getConsequentPixels(uniquePixels) : uniquePixels;
            groupToUniquePixels.put(group, pixelsList);
        }));

        final Map.Entry<T, Collection<Integer>> largestUniquePixelsGroup = groupToUniquePixels.entrySet().stream()
                .max(Comparator.comparing(e -> e.getValue().size()))
                .orElseThrow();


        if (largestUniquePixelsGroup.getValue().isEmpty()) {
            if (!tryConsequentFirst) {
                final String groupsDescription = groups.keySet().stream().map(Object::toString).collect(Collectors.joining(", "));
                throw new RuntimeException("No unique pixels for any group in " + groupsDescription);
            }

            findAndPrintKeyPixels(groups, false, scanSize);
        } else {
            final int firstUniquePixel = largestUniquePixelsGroup.getValue().iterator().next();
            System.out.println(largestUniquePixelsGroup.getKey() + ", (" + firstUniquePixel % scanSize + ", " + firstUniquePixel / scanSize + ")");
            groups.remove(largestUniquePixelsGroup.getKey());

            if (!groups.isEmpty()) {
                findAndPrintKeyPixels(groups, true, scanSize);
            }
        }
    }

    private static List<Integer> getConsequentPixels(SortedSet<Integer> suitUniquePixels) {
        final List<Integer> consequentPixels = new ArrayList<>(suitUniquePixels.size());
        int prev = Integer.MIN_VALUE;

        boolean prevWritten = false;
        for (Integer value : suitUniquePixels) {
            if (value == prev + 1) {
                if (!prevWritten) {
                    consequentPixels.add(prev);
                }
                consequentPixels.add(value);
                prevWritten = true;
            } else {
                prevWritten = false;
            }
            prev = value;
        }
        return consequentPixels;
    }
}
