package vnapps.ikara.restful.servlet;

import java.util.ArrayList;
import java.util.List;
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
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.appengine.tools.remoteapi.RemoteApiOptions;

import vnapps.ikara.model.v35.AvatarFrameModel;
import vnapps.ikara.restful.common.Utils;

@Path("/avatarFrameResfull")
public class AvatarFrameResfull {
 private static final Logger log = Logger.getLogger(AvatarFrameResfull.class.getName());
    
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
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public List<AvatarFrameModel> getAll() {
        List<AvatarFrameModel> avatarFrameModels = new ArrayList<>();
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        Query query = new Query("AvatarFrame");
        
        List<Entity> entities = datastoreService.prepare(query).asList(FetchOptions.Builder.withDefaults());
        for(Entity entity: entities) {
            AvatarFrameModel avatarFrameModel = new AvatarFrameModel();
            avatarFrameModel.idPublic = Utils.getLong(entity.getProperty("idPublic"), 0);
            avatarFrameModel.name = Utils.getString(entity.getProperty("name"), "");
            avatarFrameModel.resourceUrl = Utils.getString(entity.getProperty("resourceUrl"), "");
            avatarFrameModel.previewImg = Utils.getString(entity.getProperty("previewImg"), "");
            avatarFrameModels.add(avatarFrameModel);
        }
        
        return avatarFrameModels;
    }
    
//    public static void main(String[] args) throws Exception {    
//        
//        RemoteApiOptions options = new RemoteApiOptions().server("ikara4m.appspot.com", 443)
//                .remoteApiPath("/remote-api").useServiceAccountCredential("ikara4m@appspot.gserviceaccount.com", "D:\\PROJECT\\ikara4m-1d7122c21b97.p12");
//        RemoteApiInstaller installer = new RemoteApiInstaller();
//
//
//  try {
//      installer.install(options);
//      AvatarFrameResfull avatarFrameResfull = new AvatarFrameResfull();
//      List<AvatarFrameModel> avatarFrameModels = avatarFrameResfull.getAll();
//      System.out.println(avatarFrameModels);
//
//  } catch (Exception e) {
//      e.printStackTrace();
//  } finally {
//      installer.uninstall();
//
//  }
//}
}
