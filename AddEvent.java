package vnapps.ikara.servlet.v35;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.appengine.tools.remoteapi.RemoteApiOptions;

import vnapps.ikara.common.v0.Const;
import vnapps.ikara.common.v19.DigitalSignature;
import vnapps.ikara.common.v32.Utils;
import vnapps.ikara.model.v33.SearchSongsRequest;
import vnapps.ikara.model.v33.SearchSongsResponse;
import vnapps.ikara.model.v33.Song;
import vnapps.ikara.model.v35.AddEventModelRequest;
import vnapps.ikara.model.v35.AddEventRequest;
import vnapps.ikara.model.v35.AddEventResponse;
import vnapps.ikara.servlet.controller.CupEventManager;
import vnapps.ikara.servlet.v33.SearchSongs;
import vnapps.ikara.servlet.web.UpdateCup;

@WebServlet("/v35.AddEvent")
public class AddEvent extends HttpServlet {

    private static final Logger log = Logger.getLogger(UpdateCup.class.getName());
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException ,IOException {
        doPost(req, resp);
    };
    
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AddEventResponse addEventResponse = new AddEventResponse();
        addEventResponse.status = "FAILED";
        addEventResponse.message = "UNKNOWERR";
        try {
            String parametersInString = DigitalSignature.decryption(req.getParameter("parameters"));
//            AddEventRequest parameters = Utils.deserialize(AddEventRequest.class, parametersInString);
            CupEventManager cupEventManager = new CupEventManager(true);
            
            AddEventModelRequest parameters = Utils.deserialize(AddEventModelRequest.class, parametersInString);
            
            boolean b = cupEventManager.addEventModel(parameters);
            
            if (b) {
                addEventResponse.status = "OK";
                addEventResponse.message = "Thành công!";
                // TaskOptions task = TaskOptions.Builder.withUrl("/v35.ReloadEvent");
                //     Queue queue = QueueFactory.getDefaultQueue();
                //     queue.add(task);
            } else {
                addEventResponse.status = "FAILED";
                addEventResponse.message = "Lỗi khởi tạo!";
            }

            log.log(Level.INFO, "OK roi!!");
        } catch (Exception e) {
            e.printStackTrace();
            addEventResponse.status = "FAILED";
            addEventResponse.message = e.toString();
        } finally {
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write(Utils.serialize(addEventResponse));
        }
    }

  
    public static void main(String[] args) throws Exception {
        Utils.setEnv("GOOGLE_APPLICATION_CREDENTIALS", "/Volumes/DATA/ikara4m.json");

        RemoteApiOptions options = new RemoteApiOptions().server("ikara4m.appspot.com", 443)
                .remoteApiPath("/remote-api").useServiceAccountCredential("ikara4m@appspot.gserviceaccount.com", "D:\\PROJECT\\ikara4m-1d7122c21b97.p12");
        
        RemoteApiInstaller installer = new RemoteApiInstaller();

        try {
            installer.install(options);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServerName("www.ikara.co");

            SearchSongsRequest originalRequest = new SearchSongsRequest();
            originalRequest.cursor = null;
            originalRequest.language = "vi";
            originalRequest.platform = "ANDROID";
            originalRequest.userId = "00000000-67dc-aecb-0000-000067dcaecb";
            originalRequest.query = "Hà";
            
            originalRequest.userId = "ffffffff-85e0-d285-ffff-ffff85e0d285";
            String paramString = DigitalSignature.encryption(Utils.serialize(originalRequest));
            request.setParameter("parameters", paramString);

            AddEvent servlet = new AddEvent();
            
            servlet.doPost(request, new MockHttpServletResponse());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            installer.uninstall();

        }
    }
}
