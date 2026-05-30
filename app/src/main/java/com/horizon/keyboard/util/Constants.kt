package com.horizon.keyboard.util

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized constants for the Horizon Keyboard application.
 * No more magic numbers scattered throughout the codebase.
 */
object Constants {
    
    // Keyboard Dimensions
    object Keyboard {
        val CONTAINER_MAX_WIDTH = 896.dp
        val CONTAINER_PADDING_HORIZONTAL = 8.dp
        val CONTAINER_PADDING_TOP = 8.dp
        val CONTAINER_PADDING_BOTTOM = 32.dp
        val ROW_SPACING = 8.dp
        val KEY_SPACING = 6.dp
        val KEY_HEIGHT = 40.dp
        val KEY_CORNER_RADIUS = 6.dp
        val BORDER_WIDTH = 1.dp
    }
    
    // Key Sizes - Standardized
    object KeySize {
        // Letter keys
        val LETTER_WIDTH = 32.dp
        val LETTER_WIDTH_MEDIUM = 34.dp
        val LETTER_WIDTH_SMALL = 30.dp
        
        // Special keys
        val SHIFT_WIDTH = 42.dp
        val BACKSPACE_WIDTH = 42.dp
        val ENTER_WIDTH = 50.dp
        val SPACE_WIDTH = 160.dp
        val COMMA_WIDTH = 36.dp
        val PERIOD_WIDTH = 36.dp
        val NUMBERS_WIDTH = 50.dp
        val ABC_WIDTH = 50.dp
        
        // Number/symbol keys
        val SYMBOL_WIDTH = 32.dp
        val SYMBOL_WIDTH_SMALL = 28.dp
    }
    
    // Toolbar
    object Toolbar {
        val HEIGHT = 36.dp
        val ICON_SIZE = 20.dp
        val PADDING_HORIZONTAL = 8.dp
        val BORDER_WIDTH = 1.dp
    }
    
    // Suggestion Row
    object Suggestion {
        val ROW_HEIGHT = 36.dp
        val DIVIDER_HEIGHT = 18.dp
        val DIVIDER_WIDTH = 1.dp
        val TEXT_SIZE = 14.sp
    }
    
    // Voice
    object Voice {
        val BUTTON_PADDING_HORIZONTAL = 16.dp
        val BUTTON_PADDING_VERTICAL = 8.dp
        val BUTTON_CORNER_RADIUS = 8.dp
        val ICON_SIZE = 18.dp
        val TEXT_SIZE = 14.sp
        val STATUS_TEXT_SIZE = 13.sp
    }
    
    // Text Field
    object TextField {
        val CORNER_RADIUS = 12.dp
        val TITLE_SIZE = 24.sp
        val BODY_SIZE = 16.sp
        val PLACEHOLDER_SIZE = 16.sp
    }
    
    // Animation
    object Animation {
        const val KEY_PRESS_DURATION = 50
        const val KEY_PRESS_SCALE = 0.96f
    }
    
    // Voice Language Codes
    object Language {
        const val ENGLISH_CODE = "en-US"
        const val BANGLA_CODE = "bn-BD"
        const val ENGLISH_DISPLAY = "English"
        const val BANGLA_DISPLAY = "বাংলা"
    }
}
