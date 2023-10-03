package dev.wuason.storagemechanic.items;

import dev.wuason.mechanics.Mechanics;

import dev.wuason.mechanics.compatibilities.AdapterManager;
import dev.wuason.mechanics.items.ItemBuilderMechanic;
import dev.wuason.mechanics.utils.AdventureUtils;
import dev.wuason.storagemechanic.StorageMechanic;
import dev.wuason.storagemechanic.customblocks.CustomBlock;

import dev.wuason.storagemechanic.inventory.inventories.SearchItem.SearchType;
import dev.wuason.storagemechanic.items.properties.*;
import dev.wuason.storagemechanic.utils.StorageUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ItemInterfaceManager {

    private StorageMechanic core;

    private ArrayList<ItemInterface> itemsInterface;

    public ItemInterfaceManager(StorageMechanic core) {
        this.core = core;
    }



    public void loadItemsInterface(){

        itemsInterface = new ArrayList<>();

        File base = new File(Mechanics.getInstance().getManager().getMechanicsManager().getMechanic(core).getDirConfig().getPath() + "/itemInterfaces/");
        base.mkdirs();

        File[] files = Arrays.stream(base.listFiles()).filter(f -> {

            if(f.getName().contains(".yml")) return true;

            return false;

        }).toArray(File[]::new);

        for(File file : files){

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            ConfigurationSection sectionItemsInterfaces = config.getConfigurationSection("Items");

            if(sectionItemsInterfaces != null){
                for(Object key : sectionItemsInterfaces.getKeys(false).toArray()){

                    ConfigurationSection sectionItemInterface = sectionItemsInterfaces.getConfigurationSection((String)key);
                    ItemInterfaceType itemInterfaceType = null;
                    try {
                        itemInterfaceType = itemInterfaceType.valueOf(sectionItemInterface.getString("itemType").toUpperCase());
                    } catch (IllegalArgumentException e) {
                        AdventureUtils.sendMessagePluginConsole(core, "<red>Error loading Item interface! itemInterface_id: " + key + " in file: " + file.getName());
                        AdventureUtils.sendMessagePluginConsole(core, "<red>Error: ItemType is null");
                        continue;
                    }

                    if(itemInterfaceType == null){
                        AdventureUtils.sendMessagePluginConsole(core, "<red>Error loading Item interface! itemInterface_id: " + key + " in file: " + file.getName());
                        AdventureUtils.sendMessagePluginConsole(core, "<red>Error: ItemType is null");
                        continue;
                    }

                    String item = sectionItemInterface.getString("item");

                    if(item == null){
                        AdventureUtils.sendMessagePluginConsole(core, "<red>Error loading Item interface! itemInterface_id: " + key + " in file: " + file.getName());
                        AdventureUtils.sendMessagePluginConsole(core, "<red>Error: item is null");
                        continue;
                    }
                    if(!Mechanics.getInstance().getManager().getAdapterManager().existAdapterID(item)){
                        AdventureUtils.sendMessagePluginConsole(core, "<red>Error loading Item interface! itemInterface_id: " + key + " in file: " + file.getName());
                        AdventureUtils.sendMessagePluginConsole(core, "<red>Error: item is null");
                        continue;
                    }
                    Properties properties = null;

                    switch (itemInterfaceType){
                        case ACTION -> {
                            String actionId = sectionItemInterface.getString("properties.action_id");
                            if(actionId == null || !core.getManagers().getActionConfigManager().getActionConfigHashMap().containsKey(actionId)){
                                AdventureUtils.sendMessagePluginConsole(core, "<red>Error loading Item interface! itemInterface_id: " + key + " in file: " + file.getName());
                                AdventureUtils.sendMessagePluginConsole(core, "<red>Error: Action id is null or invalid!");
                                continue;
                            }
                            properties = new ActionItemProperties(actionId);
                        }
                        case PLACEHOLDER -> {
                            List<String> whitelistItems = sectionItemInterface.getStringList("properties.whitelist.list");
                            List<String> blacklistItems = sectionItemInterface.getStringList("properties.blacklist.list");
                            boolean whitelistEnabled = sectionItemInterface.getBoolean("properties.whitelist.enabled", false);
                            boolean blacklistEnabled = sectionItemInterface.getBoolean("properties.blacklist.enabled", false);
                            if(whitelistItems == null) whitelistEnabled = false;
                            if(blacklistItems == null) blacklistEnabled = false;
                            AdapterManager adapterManager = Mechanics.getInstance().getManager().getAdapterManager();
                            //COMPUTE ITEMS
                            List<String> itemsBlackListComputed = new ArrayList<>();
                            if(blacklistEnabled){
                                for(String i : blacklistItems){
                                    itemsBlackListComputed.add(adapterManager.getAdapterID(adapterManager.getItemStack(i)));
                                }
                            }
                            List<String> itemsWhiteListComputed = new ArrayList<>();
                            if(whitelistEnabled){
                                for(String i : whitelistItems){
                                    itemsWhiteListComputed.add(adapterManager.getAdapterID(adapterManager.getItemStack(i)));
                                }
                            }
                            properties = new PlaceHolderItemProperties(whitelistEnabled,blacklistEnabled,itemsWhiteListComputed,itemsBlackListComputed);
                        }
                        case CLEAN_ITEM -> {
                            List<String> pagesString = sectionItemInterface.getStringList("properties.pages");
                            List<String> slotsString = sectionItemInterface.getStringList("properties.slots");
                            if(slotsString == null || pagesString == null){
                                AdventureUtils.sendMessagePluginConsole(core, "<red>Error loading Item interface! itemInterface_id: " + key + " in file: " + file.getName());
                                AdventureUtils.sendMessagePluginConsole(core, "<red>Error: CLEAN_ITEM slots or pages is invalid!");
                                continue;
                            }
                            properties = new CleanItemProperties(StorageUtils.configFill(pagesString),StorageUtils.configFill(slotsString));
                        }
                        case SEARCH_ITEM -> {
                            String invId = sectionItemInterface.getString("properties.inv_id","searchItem");
                            String invResultId = sectionItemInterface.getString("properties.inv_result_id","searchItemResult");
                            String type = sectionItemInterface.getString("properties.type","name");
                            SearchType searchType = null;
                            try {
                                searchType = SearchType.valueOf(type.toUpperCase(Locale.ENGLISH));
                            }
                            catch (Exception e){
                                AdventureUtils.sendMessagePluginConsole(core, "<red>Error loading Item interface! itemInterface_id: " + key + " in file: " + file.getName());
                                AdventureUtils.sendMessagePluginConsole(core, "<red>Error: SearchType is invalid");
                                continue;
                            }
                            properties = new SearchItemProperties(invId,invResultId,searchType);
                        }
                    }

                    String displayName = sectionItemInterface.getString("displayName");

                    List<String> lore = sectionItemInterface.getStringList("lore");

                    ItemInterface itemInterface = new ItemInterface(item,displayName,lore,itemInterfaceType,(String)key,properties);

                    itemsInterface.add(itemInterface);

                }
            }
        }

        AdventureUtils.sendMessagePluginConsole(core, "<aqua> Items Interface loaded: <yellow>" + itemsInterface.size());

    }

    public ItemInterface getItemInterfaceById(String id) {
        for (ItemInterface itemInterface : itemsInterface) {
            if (itemInterface.getId().equals(id)) {
                return itemInterface;
            }
        }
        return null;
    }

    public ItemInterface getItemInterfaceByItemStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer itemDataContainer = itemMeta.getPersistentDataContainer();
        if (itemDataContainer.has(new NamespacedKey(StorageMechanic.getInstance(), "storagemechanicitem"), PersistentDataType.STRING)) {
            String id = itemDataContainer.get(new NamespacedKey(StorageMechanic.getInstance(), "storagemechanicitem"), PersistentDataType.STRING);
            return getItemInterfaceById(id);
        }
        return null;
    }
    public ItemInterface getItemInterfaceByItemStackPlaceholder(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer itemDataContainer = itemMeta.getPersistentDataContainer();
        if (itemDataContainer.has(PlaceHolderItemProperties.NAMESPACED_KEY_INTERFACE, PersistentDataType.STRING)) {
            String id = itemDataContainer.get(PlaceHolderItemProperties.NAMESPACED_KEY_INTERFACE, PersistentDataType.STRING);
            return getItemInterfaceById(id);
        }
        return null;
    }

    public List<ItemInterface> getItemInterfacesByType(ItemInterfaceType type) {
        List<ItemInterface> filteredItems = new ArrayList<>();
        for (ItemInterface itemInterface : itemsInterface) {
            if (itemInterface.getItemInterfaceType() == type) {
                filteredItems.add(itemInterface);
            }
        }
        return filteredItems;
    }

    public boolean existsItemInterface(String id) {
        for (ItemInterface itemInterface : itemsInterface) {
            if (itemInterface.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public boolean isItemInterface(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer itemDataContainer = itemMeta.getPersistentDataContainer();

        return itemDataContainer.has(new NamespacedKey(StorageMechanic.getInstance(), "storagemechanicitem"), PersistentDataType.STRING);
    }
    public boolean isItemInterfaceWithPlaceHolderItem(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer itemDataContainer = itemMeta.getPersistentDataContainer();

        return itemDataContainer.has(PlaceHolderItemProperties.NAMESPACED_KEY_INTERFACE, PersistentDataType.STRING);
    }

    public List<ItemInterface> getAllItemInterfaces() {
        return new ArrayList<>(itemsInterface);
    }

}
