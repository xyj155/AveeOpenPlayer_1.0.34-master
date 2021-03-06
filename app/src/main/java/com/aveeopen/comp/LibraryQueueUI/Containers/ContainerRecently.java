/*
 * Copyright 2019 Avee Player. All rights reserved.
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

package com.aveeopen.comp.LibraryQueueUI.Containers;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.aveeopen.Common.MediaStoreUtils;
import com.aveeopen.Common.UtilsMusic;
import com.aveeopen.comp.AppPreferences.AppPreferences;
import com.aveeopen.comp.Common.IGeneralItemContainerIdentifier;
import com.aveeopen.comp.playback.Song.PlaylistSong;
import com.aveeopen.Design.SortDesign;

import java.util.ArrayList;
import java.util.List;

public class ContainerRecently extends ContainerSongs {

    public ContainerRecently(Context context, String libraryAddress, String displayName, int displayIconResId, int pageIndex) {
        super(context, getTrackList(context, pageIndex, makeContainerIdentifier(libraryAddress)), libraryAddress, displayName, displayIconResId, pageIndex, false);
    }

    static List<PlaylistSong> getTrackList(Context context, int pageIndex, IGeneralItemContainerIdentifier containerId) {
        int numWeeks = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_recentlyAddedWeeks);
        int X = numWeeks * (3600 * 24 * 7);

        ContentResolver cr = context.getContentResolver();

        String where = MediaStore.MediaColumns.DATE_ADDED + ">" + (System.currentTimeMillis() / 1000 - X);
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        final String[] columns = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA};

        SortDesign.SortDesc sortDesc = onRequestCurrentSortDesc.invoke(pageIndex, containerId, null);

        String orderBy = MediaStoreUtils.getOrderBy(sortDesc);

        Cursor cursor = MediaStoreUtils.querySafe(cr, uri, columns, where, null, orderBy);

        List<PlaylistSong> songs;
        if (cursor != null) {
            songs = UtilsMusic.songListFromCursor(cursor);
            cursor.close();
        } else {
            songs = new ArrayList<>();
        }

        return songs;
    }

}
