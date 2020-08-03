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
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.aveeopen.Common.MediaStoreUtils;
import com.aveeopen.Common.Tuple2;
import com.aveeopen.Common.UtilsMusic;
import com.aveeopen.comp.Common.IGeneralItemContainerIdentifier;
import com.aveeopen.comp.ContextualActionBar.ActionListenerBase;
import com.aveeopen.comp.LibraryQueueUI.Containers.Adapter.HeaderFooterAdapterData;
import com.aveeopen.comp.LibraryQueueUI.Containers.Adapter.ViewAdapter;
import com.aveeopen.comp.LibraryQueueUI.Containers.Base.CursorContainerBase;
import com.aveeopen.comp.LibraryQueueUI.ContextualActions.ItemActionsSongs;
import com.aveeopen.comp.LibraryQueueUI.ViewHolders.ContentItemViewHolder;
import com.aveeopen.comp.LibraryQueueUI.ViewHolders.ViewHolderFactory;
import com.aveeopen.comp.playback.Song.PlaylistSong;
import com.aveeopen.Design.SortDesign;
import com.aveeopen.MainActivity;
import com.aveeopen.R;

import java.util.ArrayList;
import java.util.List;

public class ContainerArtists extends CursorContainerBase {

    private static final int primaryActionIndex = -1;
    private static final int defaultActionIndex = 0;

    ActionListenerBase[] itemListenerActionsSongs = new ActionListenerBase[]
            {
                    new ItemActionsSongs.PlaySingleItemAction.PlaySingleActionListener2() {
                        @Override
                        protected void onPlaySingle(Context context, Object objItem, List<PlaylistSong> songsOut) {
                            ThisItemIdentifier item = (ThisItemIdentifier) objItem;
                            getChildItems(context, "" + item.id, songsOut);
                        }
                    },

                    new ItemActionsSongs.PlayMultiItemAction.PlayMultiActionListener2() {
                        @Override
                        protected void onPlayMulti(Context context, Object objItem, List<PlaylistSong> songsOut) {
                            ThisItemIdentifier item = (ThisItemIdentifier) objItem;
                            getChildItems(context, "" + item.id, songsOut);
                        }
                    },

                    new ItemActionsSongs.ItemActionEnqueue.EnqueueActionListener2() {
                        @Override
                        protected void onEnqueue(Context context, Object objItem, List<PlaylistSong> songsOut) {
                            ThisItemIdentifier item = (ThisItemIdentifier) objItem;
                            getChildItems(context, "" + item.id, songsOut);
                            //_songsOut.add(item.song);
                        }
                    },

                    new ItemActionsSongs.ItemActionEnqueueNext.EnqueueNextActionListener2() {
                        @Override
                        protected void onEnqueue(Context context, Object objItem, List<PlaylistSong> songsOut) {
                            ThisItemIdentifier item = (ThisItemIdentifier) objItem;
                            getChildItems(context, "" + item.id, songsOut);
                        }
                    },

                    new ItemActionsSongs.SendToItemAction.SendToActionListener() {
                        @Override
                        protected void onSendTo(Context context, Object objItem, List<PlaylistSong> songsOut) {
                            ThisItemIdentifier item = (ThisItemIdentifier) objItem;
                            getChildItems(context, "" + item.id, songsOut);
                        }
                    },
            };

    public ContainerArtists(Context context, String libraryAddress, String displayName, int displayIconResId, int pageIndex) {
        super(context, libraryAddress, displayName, displayIconResId, pageIndex);
        init(context);
    }

    static Tuple2<Cursor, String> makeCursor(Context context, final IGeneralItemContainerIdentifier containerIdentifier, final int pageIndex) {
        ContentResolver cr = context.getContentResolver();

        String where = null;
        String whereVal[] = null;
        String searchText = onRequestSearchQuery.invoke(pageIndex, containerIdentifier, "");
        if (searchText != null && !searchText.isEmpty()) {
            where = MediaStore.Audio.Artists.ARTIST + " LIKE ?";
            String whereValue[] = {
                    "%" + searchText + "%"
            };
            whereVal = whereValue;
        } else {
            searchText = "";
        }

        final Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;

        final String[] columns = {
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS};

        Cursor cursor = MediaStoreUtils.querySafeEmpty(cr, uri, columns, where, whereVal, null);

        return new Tuple2<>(cursor, searchText);
    }

    @Override
    public ViewAdapter createAdapter(Context context, int type) {
        ViewAdapter.IAdapterDataProvider adapterDataProvider = new HeaderFooterAdapterData(this, this, ViewHolderFactory.VIEW_HOLDER_artists, ViewHolderFactory.VIEW_HOLDER_footer1);
        return new ViewAdapter(adapterDataProvider, this);
    }

    List<PlaylistSong> getChildItems(Context context, String artistId, List<PlaylistSong> dest) {
        ContentResolver cr = context.getContentResolver();

        String[] columns = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA};
        String where = MediaStore.Audio.Media.ARTIST_ID
                + "=?";
        String whereVal[] = {artistId};
        SortDesign.SortDesc sortDesc = onRequestCurrentSortDesc.invoke(pageIndex, getSelectionContainerIdentifier(), null);
        String orderBy = MediaStoreUtils.getOrderBy(sortDesc);

        Cursor cursor = MediaStoreUtils.querySafe(cr,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns, where,
                whereVal, orderBy);

        if (cursor != null) {
            dest = UtilsMusic.songListFromCursor(cursor, dest);
            cursor.close();
        } else {
            dest = new ArrayList<>();
        }

        return dest;
    }

    @Override
    public String getItemPositionToItemAddress(int position) {
        final Cursor item = this.getItem(position);
        return item.getString(0);//ID
    }

    @Override
    public ViewAdapter createChildAdapter(Context context, String artistId) {
        ContentResolver cr = context.getContentResolver();

        String dispName = "";
        {
            Cursor cursor2;

            final Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
            final String[] columns2 = {
                    MediaStore.Audio.Artists._ID,
                    MediaStore.Audio.Artists.ARTIST};

            String where = MediaStore.Audio.Artists._ID + "=?";
            String whereVal[] = {artistId};

            cursor2 = MediaStoreUtils.querySafe(cr, uri, columns2, where, whereVal, null);
            if (cursor2 != null) {
                cursor2.moveToFirst();
                dispName = MediaStoreUtils.CursorGetStringSafe(cursor2, 1);
                cursor2.close();
            }
        }

        ContainerSongs dataAdapter = new ContainerSongs(context,
                getChildItems(context, artistId, null),
                makeChildAddress(artistId),
                dispName,
                0,
                pageIndex,
                false);

        dataAdapter.setLibraryContainerDataListener(libraryContainerDataListenerWeak);
        return dataAdapter.createOrGetAdapter(context, MainActivity.LIBRARY_PAGE_INDEX);
    }

    @Override
    public int getItemViewType(int position)
    {
        return ViewHolderFactory.VIEW_HOLDER_libContent;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        final Cursor item = this.getItem(position);
        ContentItemViewHolder holder = (ContentItemViewHolder) viewHolder;
        holder.itemPosition = position;
        getView(item, position, holder);
    }

    public void getView(final Cursor item, int position, final ContentItemViewHolder holder) {
        holder.setToDefault(this, new ThisItemIdentifier(item.getLong(0)), this.getSelectionContainerIdentifier());
        boolean selected = onRequestContainsItemSelection.invoke(holder.itemSelection, false);
        holder.viewItemBg.setSelected(selected);
        holder.setItemActions2(itemListenerActionsSongs, primaryActionIndex, defaultActionIndex, this);
        holder.imgArt.setVisibility(View.GONE);
        holder.setImageDrawable(null);
        holder.txtNum.setVisibility(View.GONE);
        holder.txtItemLine1.setText(item.getString(1));
        holder.txtItemLine1.setTextColor(this.color);
        String content2Str = holder.txtItemLine2.getResources().getQuantityString(
                R.plurals.albums_count, item.getInt(3), item.getInt(3));
        holder.txtItemLine2.setText(content2Str);
        holder.txtItemLine2.setVisibility(View.VISIBLE);
        holder.txtItemDuration.setText(item.getString(2));
    }

    @Override
    public void getSearchOptions(Context context, String[] outSearchHint, IGeneralItemContainerIdentifier[] outContainerIdentifier) {
        outSearchHint[0] = context.getResources().getString(R.string.libContainer_Artists_search);
        outContainerIdentifier[0] = getSelectionContainerIdentifier();
    }

    @Override
    public Tuple2<Cursor, String> createOrGetCursor(Context context, String query) {
        return makeCursor(context, getSelectionContainerIdentifier(), pageIndex);
    }

    @Override
    public Tuple2<Cursor, String> createOrGetCursor(Context context) {
        return makeCursor(context, getSelectionContainerIdentifier(), pageIndex);
    }

    static class ThisItemIdentifier {
        public final long id;

        public ThisItemIdentifier(long id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return (int) id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ThisItemIdentifier && id == ((ThisItemIdentifier) o).id;
        }
    }
}
