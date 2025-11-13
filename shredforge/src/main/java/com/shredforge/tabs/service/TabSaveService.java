package com.shredforge.tabs.service;

import com.shredforge.core.model.SongRequest;
import com.shredforge.core.model.TabData;
import com.shredforge.tabs.dao.TabDataDao;
import com.shredforge.tabs.model.SavedTabSummary;
import com.shredforge.tabs.model.TabSelection;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class TabSaveService {

    private final TabDataDao dao;

    public TabSaveService(TabDataDao dao) {
        this.dao = dao;
    }

    public SavedTabSummary save(TabSelection selection, TabData tabData) {
        TabDataDao.SavedTabDto dto = dao.saveTab(selection, tabData);
        return toSummary(dto);
    }

    public List<SavedTabSummary> listSavedTabs() {
        return dao.listSavedTabs().stream().map(this::toSummary).collect(Collectors.toList());
    }

    public Set<String> savedTabIds() {
        return new HashSet<>(dao.listSavedTabs().stream().map(TabDataDao.SavedTabDto::tabId).toList());
    }

    public Optional<TabData> findTabBySong(SongRequest request) {
        return dao.listSavedTabs().stream()
                .filter(dto -> matches(dto.selection(), request))
                .findFirst()
                .map(this::toTabData);
    }

    public Optional<TabSelection> findSelection(String tabId) {
        return dao.loadSavedTab(tabId).map(TabDataDao.SavedTabDto::selection);
    }

    public Optional<TabData> loadTab(String tabId) {
        return dao.loadSavedTab(tabId).map(this::toTabData);
    }

    private SavedTabSummary toSummary(TabDataDao.SavedTabDto dto) {
        return new SavedTabSummary(dto.selection(), dto.savedAt(), dto.location());
    }

    private TabData toTabData(TabDataDao.SavedTabDto dto) {
        Path cachedLocation = dto.location();
        return new TabData(
                TabGetService.buildSourceId(dto.selection()),
                dto.song(),
                dto.rawContent(),
                dto.fetchedAt(),
                cachedLocation);
    }

    private static boolean matches(TabSelection selection, SongRequest request) {
        boolean titleMatch = request.title() == null
                || selection.title().equalsIgnoreCase(request.title().trim());
        boolean artistMatch = request.artist() == null
                || selection.artist().equalsIgnoreCase(request.artist().trim());
        return titleMatch && artistMatch;
    }
}
