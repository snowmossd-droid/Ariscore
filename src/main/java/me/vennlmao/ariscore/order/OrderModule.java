package me.vennlmao.ariscore.order;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.order.commands.OrderCommand;
import me.vennlmao.ariscore.order.gui.CollectItemsGUI;
import me.vennlmao.ariscore.order.gui.ConfirmCancelGUI;
import me.vennlmao.ariscore.order.gui.DeliveryGUI;
import me.vennlmao.ariscore.order.gui.EditOrderGUI;
import me.vennlmao.ariscore.order.gui.ListMaterialsGUI;
import me.vennlmao.ariscore.order.gui.NewOrderGUI;
import me.vennlmao.ariscore.order.gui.OrderViewGUI;
import me.vennlmao.ariscore.order.gui.YourOrdersGUI;
import me.vennlmao.ariscore.order.managers.DataManager;
import me.vennlmao.ariscore.order.managers.MaterialsManager;
import me.vennlmao.ariscore.order.managers.OrderConfigManager;
import me.vennlmao.ariscore.order.managers.OrderItem;
import me.vennlmao.ariscore.order.managers.OrderManager;
import me.vennlmao.ariscore.order.managers.SignManager;
import me.vennlmao.ariscore.order.managers.SoundManager;
import me.vennlmao.ariscore.order.managers.TimeChecker;

public class OrderModule {

    private final ArisCore plugin;
    private OrderConfigManager configManager;
    private OrderManager orderManager;
    private DataManager dataManager;
    private MaterialsManager materialsManager;
    private SoundManager soundManager;
    private SignManager signManager;
    private TimeChecker timeChecker;

    private OrderViewGUI orderViewGUI;
    private YourOrdersGUI yourOrdersGUI;
    private ListMaterialsGUI listMaterialsGUI;
    private NewOrderGUI newOrderGUI;
    private DeliveryGUI deliveryGUI;
    private CollectItemsGUI collectItemsGUI;
    private EditOrderGUI editOrderGUI;
    private ConfirmCancelGUI confirmCancelGUI;

    public OrderModule(ArisCore plugin) { this.plugin = plugin; }

    public void enable() {
        configManager    = new OrderConfigManager(plugin);
        configManager.load();

        orderManager     = new OrderManager(plugin);
        dataManager      = new DataManager(plugin);
        dataManager.init();

        for (OrderItem order : dataManager.loadAllOrders()) {
            orderManager.addOrder(order);
        }

        materialsManager = new MaterialsManager(plugin);
        materialsManager.load();

        soundManager     = new SoundManager(plugin);
        signManager      = new SignManager(plugin);

        orderViewGUI     = new OrderViewGUI(plugin);
        yourOrdersGUI    = new YourOrdersGUI(plugin);
        listMaterialsGUI = new ListMaterialsGUI(plugin);
        newOrderGUI      = new NewOrderGUI(plugin);
        deliveryGUI      = new DeliveryGUI(plugin);
        collectItemsGUI  = new CollectItemsGUI(plugin);
        editOrderGUI     = new EditOrderGUI(plugin);
        confirmCancelGUI = new ConfirmCancelGUI(plugin);

        plugin.getServer().getPluginManager().registerEvents(orderViewGUI,     plugin);
        plugin.getServer().getPluginManager().registerEvents(yourOrdersGUI,    plugin);
        plugin.getServer().getPluginManager().registerEvents(listMaterialsGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(newOrderGUI,      plugin);
        plugin.getServer().getPluginManager().registerEvents(deliveryGUI,      plugin);
        plugin.getServer().getPluginManager().registerEvents(collectItemsGUI,  plugin);
        plugin.getServer().getPluginManager().registerEvents(editOrderGUI,     plugin);
        plugin.getServer().getPluginManager().registerEvents(confirmCancelGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(signManager,      plugin);

        OrderCommand orderCmd = new OrderCommand(plugin);
        plugin.getCommand("order").setExecutor(orderCmd);
        plugin.getCommand("order").setTabCompleter(orderCmd);

        timeChecker = new TimeChecker(plugin);
        timeChecker.start();

        plugin.getLogger().info("[Order] Module enabled.");
    }

    public void disable() {
        if (timeChecker != null) timeChecker.stop();
        if (dataManager != null) dataManager.close();
        plugin.getLogger().info("[Order] Module disabled.");
    }

    public void reload() {
        configManager.load();
        materialsManager.load();
    }

    public OrderConfigManager getConfigManager()   { return configManager; }
    public OrderManager getOrderManager()          { return orderManager; }
    public DataManager getDataManager()            { return dataManager; }
    public MaterialsManager getMaterialsManager()  { return materialsManager; }
    public SoundManager getSoundManager()          { return soundManager; }
    public SignManager getSignManager()            { return signManager; }
    public OrderViewGUI getOrderViewGUI()          { return orderViewGUI; }
    public YourOrdersGUI getYourOrdersGUI()        { return yourOrdersGUI; }
    public ListMaterialsGUI getListMaterialsGUI()  { return listMaterialsGUI; }
    public NewOrderGUI getNewOrderGUI()            { return newOrderGUI; }
    public DeliveryGUI getDeliveryGUI()            { return deliveryGUI; }
    public CollectItemsGUI getCollectItemsGUI()    { return collectItemsGUI; }
    public EditOrderGUI getEditOrderGUI()          { return editOrderGUI; }
    public ConfirmCancelGUI getConfirmCancelGUI()  { return confirmCancelGUI; }
}
