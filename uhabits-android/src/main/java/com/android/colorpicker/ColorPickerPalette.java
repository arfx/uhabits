/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.colorpicker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.android.colorpicker.ColorPickerSwatch.OnColorSelectedListener;

import org.isoron.uhabits.R;

/**
 * A color picker custom view which creates an grid of color squares.  The number of squares per
 * row (and the padding between the squares) is determined by the user.
 */
public class ColorPickerPalette extends TableLayout {

    public OnColorSelectedListener mOnColorSelectedListener;

    private String mDescription;
    private String mDescriptionSelected;

    private int mSwatchLength;
    private int mMarginSize;
    private int mNumColumns;

    public ColorPickerPalette(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorPickerPalette(Context context) {
        super(context);
    }

    /**
     * Initialize the size, columns, and listener.  Size should be a pre-defined size (SIZE_LARGE
     * or SIZE_SMALL) from ColorPickerDialogFragment.
     */
    public void init(int size, int columns, OnColorSelectedListener listener) {
        mNumColumns = columns;
        Resources res = getResources();
        if (size == ColorPickerDialog.SIZE_LARGE) {
            mSwatchLength = res.getDimensionPixelSize(R.dimen.color_swatch_large);
            mMarginSize = res.getDimensionPixelSize(R.dimen.color_swatch_margins_large);
        } else {
            mSwatchLength = res.getDimensionPixelSize(R.dimen.color_swatch_small);
            mMarginSize = res.getDimensionPixelSize(R.dimen.color_swatch_margins_small);
        }
        mOnColorSelectedListener = listener;

        mDescription = res.getString(R.string.color_swatch_description);
        mDescriptionSelected = res.getString(R.string.color_swatch_description_selected);
    }

    private TableRow createTableRow() {
        TableRow row = new TableRow(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(params);
        return row;
    }

    /**
     * Adds swatches to table in a serpentine format.
     */
    public void drawPalette(int[] colors, int selectedColor) {
        if (colors == null) {
            return;
        }

        this.removeAllViews();
        int tableElements = 0;
        int rowElements = 0;
        int rowNumber = 0;

        // Fills the table with swatches based on the array of palette.
        TableRow row = createTableRow();
        for (int color : colors) {
            // The final magenta entry is the custom-color action, not a palette color.
            // Render it as a full-width rainbow button below the regular color grid.
            if (color == Color.MAGENTA) {
                if (rowElements > 0) {
                    while (rowElements != mNumColumns) {
                        addSwatchToRow(row, createBlankSpace(), rowNumber);
                        rowElements++;
                    }
                    addView(row);
                    row = createTableRow();
                    rowElements = 0;
                }
                addView(createCustomColorRow(color));
                rowNumber++;
                continue;
            }

            tableElements++;
            View colorSwatch = createColorSwatch(color, selectedColor);
            setSwatchDescription(rowNumber, tableElements, rowElements, color == selectedColor,
                    colorSwatch);
            addSwatchToRow(row, colorSwatch, rowNumber);

            rowElements++;
            if (rowElements == mNumColumns) {
                addView(row);
                row = createTableRow();
                rowElements = 0;
                rowNumber++;
            }
        }

        // Create blank views to fill the row if the last row has not been filled.
        if (rowElements > 0) {
            while (rowElements != mNumColumns) {
                addSwatchToRow(row, createBlankSpace(), rowNumber);
                rowElements++;
            }
            addView(row);
        }
    }

    private TableRow createCustomColorRow(int color) {
        TableRow row = createTableRow();
        View button = new View(getContext());
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {
                        Color.RED,
                        0xFFFF8000,
                        Color.YELLOW,
                        Color.GREEN,
                        Color.CYAN,
                        Color.BLUE,
                        0xFF8000FF,
                        Color.MAGENTA
                }
        );
        gradient.setCornerRadius(getResources().getDisplayMetrics().density * 6);
        button.setBackground(gradient);
        button.setContentDescription("Choose custom color");
        button.setOnClickListener(v -> {
            if (mOnColorSelectedListener != null) {
                mOnColorSelectedListener.onColorSelected(color);
            }
        });
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                mNumColumns * (mSwatchLength + 2 * mMarginSize) - 2 * mMarginSize,
                mSwatchLength
        );
        params.setMargins(mMarginSize, mMarginSize, mMarginSize, mMarginSize);
        row.addView(button, params);
        return row;
    }

    /**
     * Appends a swatch to the end of the row for even-numbered rows (starting with row 0),
     * to the beginning of a row for odd-numbered rows.
     */
    private static void addSwatchToRow(TableRow row, View swatch, int rowNumber) {
        if (rowNumber % 2 == 0) {
            row.addView(swatch);
        } else {
            row.addView(swatch, 0);
        }
    }

    /**
     * Add a content description to the specified swatch view. Because the palette get added in a
     * snaking form, every other row will need to compensate for the fact that the palette are added
     * in an opposite direction from their left->right/top->bottom order, which is how the system
     * will arrange them for accessibility purposes.
     */
    private void setSwatchDescription(int rowNumber, int index, int rowElements, boolean selected,
            View swatch) {
        int accessibilityIndex;
        if (rowNumber % 2 == 0) {
            // We're in a regular-ordered row
            accessibilityIndex = index;
        } else {
            // We're in a backwards-ordered row.
            int rowMax = ((rowNumber + 1) * mNumColumns);
            accessibilityIndex = rowMax - rowElements;
        }

        String description;
        if (selected) {
            description = String.format(mDescriptionSelected, accessibilityIndex);
        } else {
            description = String.format(mDescription, accessibilityIndex);
        }
        swatch.setContentDescription(description);
    }

    /**
     * Creates a blank space to fill the row.
     */
    private ImageView createBlankSpace() {
        ImageView view = new ImageView(getContext());
        TableRow.LayoutParams params = new TableRow.LayoutParams(mSwatchLength, mSwatchLength);
        params.setMargins(mMarginSize, mMarginSize, mMarginSize, mMarginSize);
        view.setLayoutParams(params);
        return view;
    }

    /**
     * Creates a color swatch.
     */
    private ColorPickerSwatch createColorSwatch(int color, int selectedColor) {
        ColorPickerSwatch view = new ColorPickerSwatch(getContext(), color,
                color == selectedColor, mOnColorSelectedListener);
        TableRow.LayoutParams params = new TableRow.LayoutParams(mSwatchLength, mSwatchLength);
        params.setMargins(mMarginSize, mMarginSize, mMarginSize, mMarginSize);
        view.setLayoutParams(params);
        return view;
    }
}
