package org.sergez.splayer.activity;

import org.sergez.splayer.util.DurationAlbumID;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.sergez.splayer.util.Constants.ROOT_PATH;

public class ListData {
  // currently showing to user file/folderlist
  private Map<String, DurationAlbumID> currentShowItems = null;
  private List<String> currentItemsFullPath = null;

  // currently playing filelist
  private List<String> currentPlayableList = null;// to separate files from folders

  private String currentPath = null;

  private String root = null;
  private List<Integer> prevListViewPosition; //to move to previous position - when returns upwards from the current folder

  public ListData() {
    currentPlayableList = new ArrayList<String>();
    currentShowItems = new LinkedHashMap<String, DurationAlbumID>(); //pairs trackName-(Duration,AlbumID)
    currentItemsFullPath = new ArrayList<String>();
    currentPath = new String();
    root = ROOT_PATH;
    prevListViewPosition = new ArrayList<Integer>();
    prevListViewPosition.add(-1);
  }

  public void clearPrevListViewPosition() {
    prevListViewPosition = new ArrayList<Integer>();
    prevListViewPosition.add(-1);
  }

  public List<Integer> getPrevListViewPosition() {
    return prevListViewPosition;
  }

  public void setPrevListViewPosition(List<Integer> prevListViewPosition) {
    this.prevListViewPosition = prevListViewPosition;
  }

  public String getRoot() {
    return root;
  }

  public void setRoot(String root) {
    this.root = root;
  }

  public DurationAlbumID getCurrentPathShowItem(String key) {
    return currentShowItems.get(key);
  }

  public String getCurrentPathShowKey(int id) {
    List<String> keys = new ArrayList<String>(currentShowItems.keySet());
    if ((id >= 0) && (id <= keys.size()))
      return keys.get(id);
    else
      throw new IndexOutOfBoundsException("Wrong id: "+ id);
  }

  public int getCurrentPathShowItemsSize() {
    return currentShowItems.size();
  }


  public int getCurrentPathPlayableListSize() {
    return currentPlayableList.size();
  }

  public String getCurrentPathItemsFullpath(int id) {
    return currentItemsFullPath.get(id);
  }

  public Map<String, DurationAlbumID> getCurrentPathShowItems() {
    return currentShowItems;
  }

  public void setCurrentPathShowItems(Map<String, DurationAlbumID> currentPathShowItems) {
    this.currentShowItems = currentPathShowItems;
  }

  public List<String> getCurrentPathPlayableList() {
    return currentPlayableList;
  }

  public void setCurrentPathPlayableList(List<String> currentPathPlayableList) {
    this.currentPlayableList = currentPathPlayableList;
  }

  public String getCurrentPath() {
    return currentPath;
  }

  public void setCurrentPath(String currentPath) {
    this.currentPath = currentPath;
  }

  public void clearCurrentPathShowItems() {
    currentShowItems = new LinkedHashMap<String, DurationAlbumID>();
  }

  public void clearCurrentPathItemsFullPath() {
    currentItemsFullPath = new ArrayList<String>();
  }

  public void clearCurrentPathPlayableList() {
    currentPlayableList = new ArrayList<String>();
  }


  public String getCurrentPathPlayableListItem(int id) {
    return currentPlayableList.get(id);
  }

  public void addCurrentPathShowItem(String track, String duration, int AlbumID) {
    DurationAlbumID durationAlbumID = new DurationAlbumID(duration, AlbumID);
    currentShowItems.put(track, durationAlbumID);
  }

  public void addCurrentPathItemsFullPath(String s) {
    currentItemsFullPath.add(s);
  }

  public void addAllCurrentPathPlayableList(List<String> input) {
    currentPlayableList.addAll(input);
  }

  public void addAllCurrentPathItemsFullPath(List<String> input) {
    currentItemsFullPath.addAll(input);
  }

  public void addCurrentPathPlayableList(String input) {
    currentPlayableList.add(input);

  }

  public void addAllCurrentPathShowItems(Map<String, DurationAlbumID> itemLocal) {
    currentShowItems.putAll(itemLocal);
  }

  public void addCurrentPathShowFolder(String folder) {
    currentShowItems.put(folder, null);
  }
}
