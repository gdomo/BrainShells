package com.example.gdomo;

import java.io.IOException;

public class KeyPixelCardRecognizerTest {
    public static void main(String[] args) throws IOException {
        KeyPixelCardRecognizer.recognizeCards(args[0], ((file, cards) -> {
            System.out.println(file.getName() + " - " + cards);
            final String expectedCards = file.getName().substring(0, file.getName().length() - ".png".length());
            if (!expectedCards.equals(cards)) {
                throw new RuntimeException("Expected: " + expectedCards + ", actual: " + cards);
            }
        }));
    }
}
