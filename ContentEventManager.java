package vnapps.ikara.servlet.web;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.appengine.tools.remoteapi.RemoteApiOptions;

import vnapps.ikara.model.v35.ShopItem;
import vnapps.ikara.restful.common.RequestData;
import vnapps.ikara.restful.common.Response;
import vnapps.ikara.restful.common.Utils;
import vnapps.ikara.restful.model.ContentEventModel;
import vnapps.ikara.restful.model.UpdateEventRequest;
import vnapps.ikara.restful.servlet.BannerFullRestfull;
import vnapps.ikara.servlet.controller.AvatarFrameManager;

@Path("/contentEvent")
public class ContentEventManager {
    
   private static final Logger log = Logger.getLogger(BannerFullRestfull.class.getName());
    
    @Context
    private HttpServletRequest httpServletRequest;
    
    @Context
    private HttpServletResponse httpServletResponse;
    
    static RemoteApiInstaller initThread() {
        System.out.println("START");
        Utils.setEnv("GOOGLE_APPLICATION_CREDENTIALS", "/Volumes/DATA/ikara4m.json");
        RemoteApiOptions options = new RemoteApiOptions().server("ikara4m.appspot.com", 443)
                .remoteApiPath("/remote-api").useServiceAccountCredential("ikara4m@appspot.gserviceaccount.com", "D:\\PROJECT\\ikara4m-1d7122c21b97.p12");
        RemoteApiInstaller installer = new RemoteApiInstaller();
        try {
            installer.install(options);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }
    
    static void finishThread(RemoteApiInstaller installer) {
        System.out.println("END");
        if (installer == null) return;
        installer.uninstall();
    }
    
    @POST
    @Path("/getFrame")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public ArrayList<ShopItem> getAvatarFrameFromDB(){
        ArrayList<ShopItem> shopItems = new ArrayList<>();
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        AvatarFrameManager avatarFrameManager = new AvatarFrameManager(datastoreService);
        shopItems = avatarFrameManager.listShopAvatarFrames;
        
        return shopItems;
    }

    
    @POST
    @Path("/deleteCup")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response deleteCupByKeyName(RequestData requestData) {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        Key key = KeyFactory.createKey("ContentEvent", requestData.id);
        datastoreService.delete(key);
        Response response = new Response();
        response.status = "OK";
        response.message = "Xóa thành công !";
        return response;
    }
    
    @POST
    @Path("/updateEvent")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response updateEvent(UpdateEventRequest updateEventRequest) {
        Response response = new Response();
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        Key key = KeyFactory.createKey("ContentEvent", updateEventRequest.idEventString);
        try {
            Entity entity = datastoreService.get(key);
            if(entity != null) {
                ContentEventModel contentEventModel1 = Utils.deserialize(ContentEventModel.class, Utils.getString(entity.getProperty("gifts_1")));
                ContentEventModel contentEventModel2 = Utils.deserialize(ContentEventModel.class, Utils.getString(entity.getProperty("gifts_2")));
                ContentEventModel contentEventModel3 = Utils.deserialize(ContentEventModel.class, Utils.getString(entity.getProperty("gifts_3")));
                
                contentEventModel1.cupUrl = updateEventRequest.cupTop1Url;
                contentEventModel1.keyCup = updateEventRequest.cupTop1;
                contentEventModel1.frameIdString = updateEventRequest.frameTop1Id;
                contentEventModel1.frameUrl = updateEventRequest.frameTop1Url;
                
                contentEventModel2.cupUrl = updateEventRequest.cupTop2Url;
                contentEventModel2.keyCup = updateEventRequest.cupTop2;
                contentEventModel2.frameIdString = updateEventRequest.frameTop2Id;
                contentEventModel2.frameUrl = updateEventRequest.frameTop2Url;
                
                contentEventModel3.cupUrl = updateEventRequest.cupTop3Url;
                contentEventModel3.keyCup = updateEventRequest.cupTop3;
                contentEventModel3.frameIdString = updateEventRequest.frameTop3Id;
                contentEventModel3.frameUrl = updateEventRequest.frameTop3Url;
                
                response.message="Update thành công";
                response.status="OK";
            }else {
                response.message="Update thất bại, vui lòng nhập đúng giá trị";
                response.status="OK";
            }
        } catch (EntityNotFoundException e) {
            response.message="Update thất bại";
            response.status="FAIL";
        }
        return response;
    }
}
