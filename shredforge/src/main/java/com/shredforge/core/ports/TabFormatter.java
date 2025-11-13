package com.shredforge.core.ports;

import com.shredforge.core.model.FormattedTab;
import com.shredforge.core.model.TabData;

/**
 * Converts raw tabs into SVG assets suitable for rendering.
 */
public interface TabFormatter {

    FormattedTab format(TabData tabData);
}
