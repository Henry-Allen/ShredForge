package com.shredforge.core.ports;

import com.shredforge.core.model.SongRequest;
import com.shredforge.core.model.TabData;

/**
 * Abstraction for fetching tabs from remote sources and storing them locally.
 */
public interface TabGateway {

    TabData fetchTab(SongRequest request);

    void persistTab(TabData tabData);
}
