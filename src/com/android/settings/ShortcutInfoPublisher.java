package com.android.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.util.Log;
import android.util.Pair;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.drawer.TileUtils;

import java.util.*;

/**
 * Helper class that publish top 5 tiles to the {@link ShortcutManager}.
 *
 * Assumptions:
 * <li>
 *     <ul> Intent short class name is used as unique identifier for creating {@link ShortcutInfo}. </ul>
 *     <ul> {@link Tile}s are sorted in ascending order of the priority field. </ul>
 * </li>
 */
public class ShortcutInfoPublisher {
    private static boolean DBG = false;
    private static String TAG = "ShortcutInfoPublisher";

    public static String PREF_FILE = "shortcut_publisher";
    public static String PREF_KEY = "shortcut_published";

    /**
     * For any {@param dShortcutMap} if any of them requires updating because {@link Tile} is different, update.
     *
     * Note: Once ShortcutInfo.getTitle supports res id, this will NOT be required on Locale change.
     * TODO: call on ACTION_LOCALE_CHANGED broadcast intent.
     */
    public static void updateShortcuts(Context context, Collection<Tile> tileList) {
        ShortcutManager sm = context.getSystemService(ShortcutManager.class);
        HashMap<String, ShortcutInfo> updateShortcutsMap = new HashMap<>();
        for (ShortcutInfo s: sm.getDynamicShortcuts()) {
            updateShortcutsMap.put(s.getId(), s);
        }
        for (ShortcutInfo s: sm.getPinnedShortcuts()) {
            updateShortcutsMap.put(s.getId(), s);
        }
        ArrayList<ShortcutInfo> updateShortcuts = new ArrayList<>();
        for(Tile t: tileList) {
            if (publishedButChanged(updateShortcutsMap, t)){
                ShortcutInfo s = buildShortcutInfo(context, t);
                updateShortcuts.add(s);
                if (DBG) Log.d(TAG, "update: " + s.getId());
            }
        }
        // This check is not required if being called from foreground activity.
        if (sm.getRemainingCallCount() > 0 && updateShortcuts.size() > 0) {
            sm.updateShortcuts(updateShortcuts);
            if (DBG) Log.d(TAG, "ShortcutManager.updateshortcuts: " + updateShortcuts.size());
        }
    }

    /**
     * Set the 5 top Tile as {@link ShortcutInfo}.
     *
     * Note: once one time loading of static list is supported, this will NOT be required.
     */
    public static void setDynamicShortcuts(Context context, Map<Pair<String, String>, Tile> tiles) {
        ShortcutManager sm = context.getSystemService(ShortcutManager.class);
        // Sort the tiles so that tiles with higher priority is at the beginning.
        ArrayList<Tile> tileList = new ArrayList<>();
        tileList.addAll(tiles.values());
        Collections.sort(tileList, TileUtils.TILE_COMPARATOR);

        int max = sm.getMaxDynamicShortcutCount();
        ArrayList<ShortcutInfo> dShortcutList = new ArrayList<>();
        for(Tile t: tileList) {
            ShortcutInfo s = buildShortcutInfo(context, t);
            dShortcutList.add(s);
            if (DBG) Log.d(TAG, "add new shortcut: " + s.getId());
            if (dShortcutList.size() >= max) {
                break;
            }
        }
        // This check is not required if being called from foreground activity.
        if (sm.getRemainingCallCount() > 0) {
            sm.setDynamicShortcuts(dShortcutList);
            if (DBG) Log.d(TAG, "ShortcutManager.setDynamicShortcuts");
            SharedPreferences sharedPref = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(PREF_KEY, true);
            editor.commit();
        }
    }

    /**
     * Extract string from {@link Tile} that should be used for shortcut id.
     */
    private static String getShortcutInfoId(Tile t) {
        return t.intent.getComponent().getShortClassName();
    }

    /**
     * Build ShortcutInfo from given Tile.
     */
    private static ShortcutInfo buildShortcutInfo(Context context, Tile t) {
        return new ShortcutInfo.Builder(context)
                .setId(getShortcutInfoId(t))
                .setTitle(t.title.toString())
                .setIntent(t.intent)
                .setIcon(Icon.createWithBitmap(CreateShortcut.createIcon(context, t.icon.getResId())))
                .setWeight(t.priority)
                .build();
    }

    private static boolean publishedButChanged(Map<String, ShortcutInfo> shortcutMap, Tile t) {
        ShortcutInfo s = shortcutMap.get(getShortcutInfoId(t));
        if (s == null) {
            return false; // never published.
        }
        if (t.priority != s.getWeight()) {
            if (DBG) Log.d(TAG, s.getId() +  " *weight* diff s:" + s.getWeight() + "t:" + t.priority);
            return true;
        } else if (!t.intent.equals(s.getIntent())) {
            if (DBG) Log.d(TAG, s.getId() + " *intent* diff s:" + s.getIntent() + "t:" + t.intent);
            return true;
        } else if (!t.title.equals(s.getTitle())) {
            if (DBG) Log.d(TAG, s.getId() + " *title* diff s:" + s.getTitle() + "t:" + t.title);
            return true;
        } else if (!t.icon.sameAs(s.getIcon())) {
            if (DBG) Log.d(TAG, s.getId() + " *icon* diff s:" + s.getIcon() + "t:" + t.icon);
            return true;
        }
        return false;
    }
}
