package com.taskmanager.view.components;

import org.junit.jupiter.api.Test;

import java.awt.BorderLayout;
import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class CardPanelTest {

    @Test
    void cardPanelInitializesNonOpaqueWithPadding() {
        CardPanel card = new CardPanel(new BorderLayout());
        assertFalse(card.isOpaque());
        assertNotNull(card.getBorder());
    }

    @Test
    void customCardPanelInitializesCorrectly() {
        CardPanel card = new CardPanel(null, Color.WHITE, Color.GRAY, 10, false);
        assertFalse(card.isOpaque());
        assertNotNull(card.getBorder());
    }
}
