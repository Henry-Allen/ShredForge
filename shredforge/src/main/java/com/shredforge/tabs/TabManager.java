package com.shredforge.tabs;

import com.shredforge.core.model.SongRequest;
import com.shredforge.core.model.TabData;
import com.shredforge.core.ports.TabGateway;
import com.shredforge.tabs.dao.TabDataDao;
import com.shredforge.tabs.model.SavedTabSummary;
import com.shredforge.tabs.model.TabSearchRequest;
import com.shredforge.tabs.model.TabSearchResult;
import com.shredforge.tabs.model.TabSelection;
import com.shredforge.tabs.service.TabGetService;
import com.shredforge.tabs.service.TabSaveService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * High-level manager that coordinates tab search, download, and persistence requests.
 */
public final class TabManager implements TabGateway {

    private final TabGetService getService;
    private final TabSaveService saveService;

    public TabManager(TabGetService getService, TabSaveService saveService) {
        this.getService = getService;
        this.saveService = saveService;
    }

    public static TabManager createDefault() {
        TabDataDao dao = new TabDataDao();
        return new TabManager(new TabGetService(dao), new TabSaveService(dao));
    }

    public List<TabSearchResult> searchTabs(TabSearchRequest request) {
        Set<String> savedIds = new HashSet<>(saveService.savedTabIds());
        return getService.search(request).stream()
                .map(selection -> new TabSearchResult(selection, savedIds.contains(selection.tabId())))
                .toList();
    }

    public List<SavedTabSummary> listSavedTabs() {
        List<SavedTabSummary> summaries = saveService.listSavedTabs();
        summaries.forEach(summary -> getService.registerSelection(summary.selection()));
        return summaries;
    }

    public TabData downloadSelection(TabSelection selection) {
        return getService.download(selection);
    }

    public TabData downloadAndSave(TabSelection selection) {
        TabData downloaded = downloadSelection(selection);
        SavedTabSummary summary = saveService.save(selection, downloaded);
        return new TabData(
                downloaded.sourceId(), downloaded.song(), downloaded.rawContent(), downloaded.fetchedAt(), summary.location());
    }

    public Optional<TabData> loadSavedTab(String tabId) {
        return saveService.loadTab(tabId);
    }

    public Optional<TabSelection> findSelection(String tabId) {
        return getService.findSelection(tabId).or(() -> saveService.findSelection(tabId));
    }

    @Override
    public TabData fetchTab(SongRequest request) {
        return saveService.findTabBySong(request)
                .or(() -> getService.downloadBySongRequest(request))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unable to locate a tab for " + request.displayName() + ". Try searching first."));
    }

    @Override
    public void persistTab(TabData tabData) {
        String tabId = TabGetService.extractTabId(tabData.sourceId());
        TabSelection selection = findSelection(tabId)
                .orElseThrow(() -> new IllegalStateException(
                        "No matching tab selection found for source " + tabData.sourceId()));
        saveService.save(selection, tabData);
    }
}
