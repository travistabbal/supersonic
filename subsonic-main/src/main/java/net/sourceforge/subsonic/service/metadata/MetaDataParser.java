/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.service.metadata;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.service.ServiceLocator;
import net.sourceforge.subsonic.service.SettingsService;


/**
 * Parses meta data from media files.
 *
 * @author Sindre Mehus
 */
public abstract class MetaDataParser {

    /**
     * Parses meta data for the given file.
     *
     * @param file The file to parse.
     * @return Meta data for the file, never null.
     */
    public MetaData getMetaData(File file) {

        MetaData metaData = getRawMetaData(file);
        String artist = metaData.getArtist();
        String album = metaData.getAlbumName();
        String title = metaData.getTitle();

        if (artist == null) {
            artist = guessArtist(file);
        }
        if (album == null) {
            album = guessAlbum(file, artist);
        }
        if (title == null) {
            title = guessTitle(file);
        }

        metaData.setArtist(artist);
        metaData.setAlbumName(album);
        metaData.setTitle(title);

        return metaData;
    }

    /**
     * Parses meta data for the given file. No guessing or reformatting is done.
     *
     *
     * @param file The file to parse.
     * @return Meta data for the file.
     */
    public abstract MetaData getRawMetaData(File file);

    /**
     * Updates the given file with the given meta data.
     *
     * @param file     The file to update.
     * @param metaData The new meta data.
     */
    public abstract void setMetaData(MediaFile file, MetaData metaData);

    /**
     * Returns whether this parser is applicable to the given file.
     *
     * @param file The file in question.
     * @return Whether this parser is applicable to the given file.
     */
    public abstract boolean isApplicable(File file);

    /**
     * Returns whether this parser supports tag editing (using the {@link #setMetaData} method).
     *
     * @return Whether tag editing is supported.
     */
    public abstract boolean isEditingSupported();

    /**
     * Guesses the artist for the given file.
     */
    public String guessArtist(File file) {
        File parent = file.getParentFile();
        if (isRoot(parent)) {
            return "";
        }
        File grandParent = parent.getParentFile();
        return isRoot(grandParent) ? "" : grandParent.getName();
    }

    /**
     * Guesses the album for the given file.
     */
    public String guessAlbum(File file, String artist) {
        File parent = file.getParentFile();
        String album = isRoot(parent) ? "" : parent.getName();
        if (artist != null) {
            album = album.replace(artist + " - ", "");
        }
        return album;
    }

    /**
     * Guesses the title for the given file.
     */
    public String guessTitle(File file) {
        return removeTrackNumberFromTitle(FilenameUtils.getBaseName(file.getPath()), null);
    }

    private boolean isRoot(File file) {
        SettingsService settings = ServiceLocator.getSettingsService();
        List<MusicFolder> folders = settings.getAllMusicFolders(false, true);
        for (MusicFolder folder : folders) {
            if (file.equals(folder.getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes any prefixed track number from the given title string.
     *
     * @param title       The title with or without a prefixed track number, e.g., "02 - Back In Black".
     * @param trackNumber If specified, this is the "true" track number.
     * @return The title with the track number removed, e.g., "Back In Black".
     */
    protected String removeTrackNumberFromTitle(String title, Integer trackNumber) {
        title = title.trim();

        // Don't remove numbers if true track number is given, and title does not start with it.
        if (trackNumber != null && !title.matches("0?" + trackNumber + "[\\.\\- ].*")) {
            return title;
        }

        String result = title.replaceFirst("^\\d{2}[\\.\\- ]+", "");
        return result.length() == 0 ? title : result;
    }
}