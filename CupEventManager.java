package vnapps.ikara.servlet.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.core.appender.rolling.action.IfFileName;
import org.json.JSONObject;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.InvalidValueException;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.repackaged.com.google.gson.Gson;
import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.appengine.tools.remoteapi.RemoteApiOptions;

import vnapps.ikara.common.v0.Language;
import vnapps.ikara.common.v19.DigitalSignature;
import vnapps.ikara.common.v35.Utils;
import vnapps.ikara.model.v27.TopSongsResponse;
import vnapps.ikara.model.v30.ContestGiftItem;
import vnapps.ikara.model.v30.ContestRules;
import vnapps.ikara.model.v30.TextContestRule;
import vnapps.ikara.model.v33.GetRulesFamilyResponse;
import vnapps.ikara.model.v33.SearchSongsRequest;
import vnapps.ikara.model.v34.GetRulesTopLiveRoomsResponse;
import vnapps.ikara.model.v35.AddEventModelRequest;
import vnapps.ikara.model.v35.AddEventRequest;
import vnapps.ikara.model.v35.AddEventResponse;
import vnapps.ikara.model.v35.GiftEventModel;
import vnapps.ikara.restful.model.ContentEventModel;
import vnapps.ikara.servlet.v35.AddEvent;
import vnapps.ikara.servlet.web.UpdateCup;
import vnapps.ikara.servlet.webModel.CupRequest;
import vnapps.ikara.servlet.webModel.CupResponse;

public class CupEventManager {
//    public String log = "";
    
    
    private static final int TYPERECORDING = 0; 
    private static final int TYPEKARA = 1; 
    private static final int TYPEFAMILY = 2; 
    
    
    
    //Active
    private ContestRules eventRecordingIkara;
    private ContestRules eventRecordingYokara;
    private ContestRules eventRecordingTimor;
    private ContestRules eventRecordingModambique;
    private ContestRules eventRecordingLao;

    private GetRulesTopLiveRoomsResponse eventKaraIkara;
    private GetRulesTopLiveRoomsResponse eventKaraYokara;
    private GetRulesTopLiveRoomsResponse eventKaraTimor;
    private GetRulesTopLiveRoomsResponse eventKaraModambique;
    private GetRulesTopLiveRoomsResponse eventKaraLao;

    
    private GetRulesFamilyResponse eventFamilyIkara;
    private GetRulesFamilyResponse eventFamilyYokara;
    private GetRulesFamilyResponse eventFamilyTimor;
    private GetRulesFamilyResponse eventFamilyModambique;
    private GetRulesFamilyResponse eventFamilyLao;
    
    private  List<Entity> memcacheFullEvent = null;
    private String keyMemcacheString = "memcacheFullEvent";
    public CupEventManager(Boolean isShow) {
        firdSetData(isShow);
    }
    
    private  void firdSetData(Boolean isShow) {
            loadMemcache();
            
            if (memcacheFullEvent == null) {
                memcacheFullEvent = loadDBtoMemcache();
            }
            setEventRecording(Language.Vietnamese);
            setEventRecording(Language.English);
            
            setEventKara(Language.Vietnamese);
            setEventKara(Language.English);
            
            setEventFamily(Language.Vietnamese);
            setEventFamily(Language.English);
            
            getEvent(isShow);
            
    }
    
    public static void main(String[] args) {
        Utils.setEnv("GOOGLE_APPLICATION_CREDENTIALS", "/Volumes/DATA/ikara4m.json");

        RemoteApiOptions options = new RemoteApiOptions().server("ikara4m.appspot.com", 443)
                .remoteApiPath("/remote-api").useServiceAccountCredential("ikara4m@appspot.gserviceaccount.com", "D:\\PROJECT\\ikara4m-1d7122c21b97.p12");
        
        RemoteApiInstaller installer = new RemoteApiInstaller();

        try {
            installer.install(options);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServerName("www.ikara.co");

            CupEventManager cupEventManager = new CupEventManager(true);
            cupEventManager.getEventFamily(Language.English);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            installer.uninstall();

        }

    }
    
    public void getEvent(Boolean isShow) {  
     List<Entity> results = memcacheFullEvent;
     for (Entity entity : results) {
        String nameString = entity.getKey().getName();
        if (nameString.contains("Defaults")) { continue;}
        Date yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);
        Date tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        
        Date timeActiveStart = Utils.getDate(entity.getProperty("timeActiveStart"), null);
        Date timeActiveEnd = Utils.getDate(entity.getProperty("timeActiveEnd"), null);
        if (timeActiveStart == null || timeActiveEnd == null) {
            continue;
        }

        timeActiveStart.setHours(timeActiveStart.getHours() + 7 - Utils.getCurrentTimezoneOffset());
        timeActiveEnd.setHours(timeActiveEnd.getHours() + 7 - Utils.getCurrentTimezoneOffset());
        Date now = new Date();
        
           //int timeDelay = 0;
           int timeFamily = 0;
           if (nameString.startsWith("Recording")) {
               timeActiveStart.setHours(Utils.getCurrentTimezoneOffset() + 14);
               timeActiveStart.setMinutes(2);
               timeActiveStart.setSeconds(1);
               timeActiveStart.setDate(timeActiveStart.getDate() -1);
               
               timeActiveEnd.setHours(Utils.getCurrentTimezoneOffset() + 14);
               timeActiveEnd.setMinutes(2);
               timeActiveEnd.setSeconds(0);
           } else if (nameString.startsWith("Kara")) {
               timeActiveStart.setHours(Utils.getCurrentTimezoneOffset() + 13);
               timeActiveStart.setMinutes(2);
               timeActiveStart.setSeconds(1);
               timeActiveStart.setDate(timeActiveStart.getDate() -1);
               
               timeActiveEnd.setHours(Utils.getCurrentTimezoneOffset() + 13);
               timeActiveEnd.setMinutes(2);
               timeActiveEnd.setSeconds(0);
           } else if (nameString.startsWith("Family")) {
               timeActiveStart.setHours(Utils.getCurrentTimezoneOffset());
               timeActiveStart.setMinutes(2);
               timeActiveStart.setSeconds(1);
               timeActiveStart.setDate(timeActiveStart.getDate() -7);
               
               timeActiveEnd.setHours(Utils.getCurrentTimezoneOffset());
               timeActiveEnd.setMinutes(2);
               timeActiveEnd.setSeconds(0);
 
           }

           if (isShow) {
                   if (now.getTime() >= timeActiveStart.getTime() && now.getTime() < timeActiveEnd.getTime()) {
                       updateEventShow(entity);
                        } 
                } else {
                    if (now.getTime() >= timeActiveStart.getTime() && now.getTime() < timeActiveEnd.getTime() + 3600000) {
                        updateEventActive(entity);
            } 
        }

    }
        
        
       
    }
    
    private void setEventRecording(String language) {
        switch (language) {
            case Language.Vietnamese:
                setEventRecordingIkara(null);
                break;
            case Language.English:
                setEventRecordingYokara(null);
                break;
            case Language.TimorLeste:
                
                break;
            case Language.Mozambique:
                
                break;
            case Language.Laos:
                
                break;
        }
    }

    private void setEventKara(String language) {
        switch (language) {
            case Language.Vietnamese:
                setEventKaraIkara(null);
                break;
            case Language.English:
                setEventKaraYokara(null);
                break;
            case Language.TimorLeste:
                
                break;
            case Language.Mozambique:
                
                break;
            case Language.Laos:
                
                break;
        }
    }

    private void setEventFamily(String language) {
        switch (language) {
            case Language.Vietnamese:
                setEventFamilyIkara(null);
                break;
            case Language.English:
                setEventFamilyYokara(null);
                break;
            case Language.TimorLeste:
                
                break;
            case Language.Mozambique:
                
                break;
            case Language.Laos:
                
                break;
        }
    }

    public ContestRules getEventRecording(String language) {
        switch (language) {
            case Language.Vietnamese:
                return eventRecordingIkara;
            case Language.English:
                return eventRecordingYokara;       
            case Language.TimorLeste:
                return eventRecordingTimor;
            case Language.Mozambique:
                return eventRecordingModambique;
            case Language.Laos:
                return eventRecordingLao;
        }
        return eventRecordingYokara;
        
    }
    
    public GetRulesTopLiveRoomsResponse getEventKara(String language) {
        switch (language) {
            case Language.Vietnamese:
                return eventKaraIkara;
            case Language.English:
                return eventKaraYokara;       
            case Language.TimorLeste:
                return eventKaraTimor;
            case Language.Mozambique:
                return eventKaraModambique;
            case Language.Laos:
                return eventKaraLao;
        }
        return eventKaraYokara;
    }
    
    public GetRulesFamilyResponse getEventFamily(String language) {
        switch (language) {
            case Language.Vietnamese:
                return eventFamilyIkara;
            case Language.English:
                return eventFamilyYokara;       
            case Language.TimorLeste:
                return eventFamilyTimor;
            case Language.Mozambique:
                return eventFamilyModambique;
            case Language.Laos:
                return eventFamilyLao;
        }
        return eventFamilyYokara;
    }

    public boolean addEvent(AddEventRequest parameters) {
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService(); 
      if (parameters.cupForDb == null
              ||( !parameters.cupForDb.equals("Recording")
              && !parameters.cupForDb.equals("Kara")
              && !parameters.cupForDb.equals("Family"))) {
          return false;
      }
      if (parameters.name == null
              || parameters.name.isEmpty()
              || parameters.name.contains("Defaults")
      ) {
          return false;
      }
      
          if((parameters.gifts_1.frameUrl == null ||parameters.gifts_1.frameUrl.equals("")) ||
                  (parameters.gifts_2.frameUrl == null ||parameters.gifts_2.frameUrl.equals("") ) ||
                  ( parameters.gifts_3.frameUrl == null ||parameters.gifts_3.frameUrl.equals(""))) {
              Key k1 = KeyFactory.createKey("ContentEvent", "RecordingDefaultsen.yokara");
              try {
               Entity thisEntity = datastore.get(k1);
               ContentEventModel contentEventModel1 = Utils.deserialize(ContentEventModel.class,Utils.getString(thisEntity.getProperty("gifts_1")));
               ContentEventModel contentEventModel2 = Utils.deserialize(ContentEventModel.class,Utils.getString(thisEntity.getProperty("gifts_2")));
               ContentEventModel contentEventModel3 = Utils.deserialize(ContentEventModel.class,Utils.getString(thisEntity.getProperty("gifts_3")));
               parameters.gifts_1.frameUrl = contentEventModel1.frameUrl;
               parameters.gifts_2.frameUrl = contentEventModel2.frameUrl;
               parameters.gifts_3.frameUrl = contentEventModel3.frameUrl;
            } catch (EntityNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
          }

      Entity entity = new Entity("ContentEvent", parameters.cupForDb + parameters.name + parameters.language);
      datastore.put(entity);
      if (parameters.language.equals("all")) {
          parameters.language = Language.Vietnamese;
      }
if (parameters.timeRule == null || parameters.mainRule == null || parameters.otherRule == null) {
      
      switch (parameters.typeString) {
        case TYPERECORDING:
            parameters.timeRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleRecording(parameters.language, 1, null)));
            parameters.mainRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleRecording(parameters.language, 2, null)));
            parameters.otherRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleRecording(parameters.language, 3, null)));
            break;

        case TYPEKARA:
            parameters.timeRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleKara(parameters.language, 1, null)));
            parameters.mainRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleKara(parameters.language, 2, null)));
            parameters.otherRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleKara(parameters.language, 3, null)));

            break;
        case TYPEFAMILY:
            parameters.timeRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleFamily(parameters.language, 1, null)));
            parameters.mainRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleFamily(parameters.language, 2, null)));
            parameters.otherRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleFamily(parameters.language, 3, null)));

            break;
        default: return false;
    }
}
      

      entity.setProperty("eventName", parameters.eventName);
      ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
      String jsonTimeRule = null;
      String jsonMainRule = null;
      String jsonOtherRule = null;
      
      String gifts1 = null;
      String gifts2 = null;
      String gifts3 = null;
      String gifts410 = null;
      try {
          jsonTimeRule = Utils.serialize(parameters.timeRule);
          jsonMainRule = Utils.serialize(parameters.mainRule);
          jsonOtherRule = Utils.serialize(parameters.otherRule);
          
          gifts1 = Utils.serialize(parameters.gifts_1);
          gifts2 = Utils.serialize(parameters.gifts_2);
          gifts3 = Utils.serialize(parameters.gifts_3);
          gifts410 = Utils.serialize(parameters.gifts_4_10);
      } catch (Exception e) {
          e.printStackTrace();
      }
      entity.setProperty("timeRule", jsonTimeRule);
      entity.setProperty("mainRule", jsonMainRule);
      entity.setProperty("otherRule", jsonOtherRule);
      
      
      
      entity.setProperty("gifts_1", gifts1);
      entity.setProperty("gifts_2", gifts2);
      entity.setProperty("gifts_3", gifts3);
      entity.setProperty("gifts_4_10", gifts410); 

      entity.setProperty("timeShowStart", parameters.timeShowStart);
      entity.setProperty("timeShowEnd", parameters.timeShowEnd);
      entity.setProperty("timeActiveStart", parameters.timeActiveStart);
      entity.setProperty("timeActiveEnd", parameters.timeActiveEnd);
      datastore.put(entity);
      memcacheFullEvent = loadDBtoMemcache();  
      return true;
    }
    
    public boolean addEventModel(AddEventModelRequest parameters) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService(); 
        if (parameters.cupForDb == null
                ||( !parameters.cupForDb.equals("Recording")
                && !parameters.cupForDb.equals("Kara")
                && !parameters.cupForDb.equals("Family"))) {
            return false;
        }
        if (parameters.name == null
                || parameters.name.isEmpty()
                || parameters.name.contains("Defaults")
        ) {
            return false;
        }
        
        if(parameters.language == null || parameters.language.equals("")) {
            return false;
        }
        
            if((parameters.gifts_1.frameUrl == null ||parameters.gifts_1.frameUrl.equals("")) ||
                    (parameters.gifts_2.frameUrl == null ||parameters.gifts_2.frameUrl.equals("") ) ||
                    ( parameters.gifts_3.frameUrl == null ||parameters.gifts_3.frameUrl.equals(""))) {
                Key k1 = KeyFactory.createKey("ContentEvent", "RecordingDefaultsen.yokara");
                try {
                 Entity thisEntity = datastore.get(k1);
                 ContentEventModel contentEventModel1 = Utils.deserialize(ContentEventModel.class,Utils.getString(thisEntity.getProperty("gifts_1")));
                 ContentEventModel contentEventModel2 = Utils.deserialize(ContentEventModel.class,Utils.getString(thisEntity.getProperty("gifts_2")));
                 ContentEventModel contentEventModel3 = Utils.deserialize(ContentEventModel.class,Utils.getString(thisEntity.getProperty("gifts_3")));
                 parameters.gifts_1.frameUrl = contentEventModel1.frameUrl;
                 parameters.gifts_1.frameIdString = contentEventModel1.frameIdString;
                 parameters.gifts_2.frameUrl = contentEventModel2.frameUrl;
                 parameters.gifts_2.frameIdString = contentEventModel2.frameIdString;
                 parameters.gifts_3.frameUrl = contentEventModel3.frameUrl;
                 parameters.gifts_3.frameIdString = contentEventModel3.frameIdString;
              } catch (EntityNotFoundException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
              }
            }

        Entity entity = new Entity("ContentEvent", parameters.cupForDb + parameters.name + parameters.language);
        datastore.put(entity);
        if (parameters.language.equals("all")) {
            parameters.language = Language.Vietnamese;
        }
  if (parameters.timeRule == null || parameters.mainRule == null || parameters.otherRule == null) {
        
        switch (parameters.typeString) {
          case TYPERECORDING:
              parameters.timeRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleRecording(parameters.language, 1, null)));
              parameters.mainRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleRecording(parameters.language, 2, null)));
              parameters.otherRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleRecording(parameters.language, 3, null)));
              break;

          case TYPEKARA:
              parameters.timeRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleKara(parameters.language, 1, null)));
              parameters.mainRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleKara(parameters.language, 2, null)));
              parameters.otherRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleKara(parameters.language, 3, null)));

              break;
          case TYPEFAMILY:
              parameters.timeRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleFamily(parameters.language, 1, null)));
              parameters.mainRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleFamily(parameters.language, 2, null)));
              parameters.otherRule = Utils.deserialize(TextContestRule.class, Utils.serialize(getTitleFamily(parameters.language, 3, null)));

              break;
          default: return false;
      }
  }
        

        entity.setProperty("eventName", parameters.eventName);
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String jsonTimeRule = null;
        String jsonMainRule = null;
        String jsonOtherRule = null;
        
        String gifts1 = null;
        String gifts2 = null;
        String gifts3 = null;
        String gifts410 = null;
        try {
            jsonTimeRule = Utils.serialize(parameters.timeRule);
            jsonMainRule = Utils.serialize(parameters.mainRule);
            jsonOtherRule = Utils.serialize(parameters.otherRule);
            
            gifts1 = Utils.serialize(parameters.gifts_1);
            gifts2 = Utils.serialize(parameters.gifts_2);
            gifts3 = Utils.serialize(parameters.gifts_3);
            gifts410 = Utils.serialize(parameters.gifts_4_10);
        } catch (Exception e) {
            e.printStackTrace();
        }
        entity.setProperty("timeRule", jsonTimeRule);
        entity.setProperty("mainRule", jsonMainRule);
        entity.setProperty("otherRule", jsonOtherRule);
        
        
        
        entity.setProperty("gifts_1", gifts1);
        entity.setProperty("gifts_2", gifts2);
        entity.setProperty("gifts_3", gifts3);
        entity.setProperty("gifts_4_10", gifts410); 

        entity.setProperty("timeShowStart", parameters.timeShowStart);
        entity.setProperty("timeShowEnd", parameters.timeShowEnd);
        entity.setProperty("timeActiveStart", parameters.timeActiveStart);
        entity.setProperty("timeActiveEnd", parameters.timeActiveEnd);
        datastore.put(entity);
        memcacheFullEvent = loadDBtoMemcache();  
        return true;
      }
    
    private   TextContestRule getTitleKara(String language, int type, String jsonContent) {
        if (type == 1) {
            TextContestRule timeRule = new TextContestRule();
            timeRule.title = "Thời gian";
            timeRule.details = "Bắt đầu lúc 20:00 mỗi ngày, kết thúc 19:59 ngày hôm sau";
            try {
                JSONObject jsonObject = new JSONObject(jsonContent);
                timeRule.title = jsonObject.getString("title");
                timeRule.details = jsonObject.getString("details");
            } catch (Exception e) {
                // TODO: handle exception
            }
            return timeRule;
        }
        
        if (type == 2) {
            TextContestRule mainRule = new TextContestRule();
            mainRule.title = "Quy tắc";
            mainRule.details = "1. Bảng xếp hạng sẽ theo tổng số điểm phòng kara nhận được. " +
                    "Trong đó điểm phòng kara nhận được sẽ dựa vào điểm quà tặng + điểm sôi nổi của phòng kara. " + "\n" + "\n" +
                    "2. Điểm quà tặng sẽ bằng 10 lần giá trị của quà tặng. Ví dụ quà tặng có giá 1 iCoin sẽ được cộng 10 điểm" + "\n" + "\n" +
                    "3. Điểm sôi nổi: mỗi thành viên online mỗi phút sẽ được + 10 điểm. " + "\n" + "\n" +
                    "4. Quà tặng sẽ được trao cho chủ phòng kara."
                    ;
            try {
                JSONObject jsonObject = new JSONObject(jsonContent);
                mainRule.title = jsonObject.getString("title");
                mainRule.details = jsonObject.getString("details");
            } catch (Exception e) {
                // TODO: handle exception
            }
            return mainRule;
        }
        
        if (type == 3) {
            TextContestRule otherRule = new TextContestRule();
            otherRule.title = "Khác";
            otherRule.details = 
                    "Trong thời gian diễn ra, nếu phát hiện người dùng nào sử dụng các thủ đoạn bất thường để đua top, chúng tôi sẽ lập tức hủy quyền dự thi, khóa tài khoản mà không cần thông báo trước!" + "\n" +
                    "Các thủ đoạn vi phạm bao gồm: " + "\n" +
                    "1. Sử dụng hành vi gian lận điểm số." + "\n" +
                    "Quyền giải thích cuối cùng thuộc về đội ngũ của iKara - Yokara";
            try {
                JSONObject jsonObject = new JSONObject(jsonContent);
                otherRule.title = jsonObject.getString("title");
                otherRule.details = jsonObject.getString("details");
            } catch (Exception e) {
                // TODO: handle exception
            }
            return otherRule;
        }
        TextContestRule none = new TextContestRule();
        return none;


    }
    
    private   TextContestRule getTitleFamily(String language, int type, String jsonContent) {
        if (type == 1) {
            TextContestRule timeRule = new TextContestRule();
            timeRule.title = "Thời gian";
            timeRule.details = "1. Bảng xếp hạng tuần là thứ hạng của các Gia tộc dựa trên tổng điểm trong tuần của gia tộc đó. Bảng xếp hạng mới sẽ được bắt đầu vào 7 giờ Chủ nhật hàng tuần.\n\n"
            + "2. Bảng xếp hạng tháng là thứ hạng của các Gia tộc dựa trên tổng số điểm trong tháng của gia tộc đó. Bảng xếp hạng mới sẽ được bắt đầu vào 7 giờ ngày đầu tiên của mỗi tháng";
            try {
                JSONObject jsonObject = new JSONObject(jsonContent);
                timeRule.title = jsonObject.getString("title");
                timeRule.details = jsonObject.getString("details");
            } catch (Exception e) {
                // TODO: handle exception
            }
            return timeRule;
        }
        
        if (type == 2) {
            TextContestRule mainRule = new TextContestRule();
            mainRule.title = "Quy tắc";
            mainRule.details = " Điểm gia tộc là tổng số Điểm sôi nổi và Điểm cống hiến mà các thành viên trong gia tộc nhận được.\n\n"
            + "1. Điểm sôi nổi được tính trong các trường hợp:\n"
            + " - Khi nhận quà từ người khác. Số điểm x10 giá trị quà tặng.\n"
            + " - Khi bài thu nhận được lượt nghe, lượt thích hoặc lượt bình luận. Với mỗi lượt thích, nghe hoặc bình luận chỉ được tính 1 lần đối với 1 bài thu. \n"
            + " - Khi online phòng kara. Online 1 phút tương ứng với 10 điểm.\n"
            + "2. Điểm cống hiến được tính trong các trường hợp:\n"
            + " - Khi tặng quà cho người khác. Số điểm x10 giá trị quà tặng.\n"
            + " - Khi thích, nghe hoặc bình luận bài thu của người khác. Vỡi mỗi lượt thích, nghe hoặc bình luận chỉ được tính 1 lần đối với 1 bài ";
            try {
                JSONObject jsonObject = new JSONObject(jsonContent);
                mainRule.title = jsonObject.getString("title");
                mainRule.details = jsonObject.getString("details");
            } catch (Exception e) {
                // TODO: handle exception
            }
            return mainRule;
        }
        
        if (type == 3) {
            TextContestRule otherRule = new TextContestRule();
            otherRule.title = "Lưu ý";
            otherRule.details = 
                    " Trong thời gian tham gia, nếu phát hiện người dùng nào sử dụng các thủ đoạn bất thường để đua top, chúng tôi sẽ lập tức hủy quyền dự thi, khóa tài khoản mà không cần thông báo trước. Các hành vi vi phạm bao gồm gian lận điểm số hoặc các hành vi tương tự.\n"
                    + " Quyền giải thích cuối cùng thuộc về đội ngũ của iKara – Yokara.";
            try {
                JSONObject jsonObject = new JSONObject(jsonContent);
                otherRule.title = jsonObject.getString("title");
                otherRule.details = jsonObject.getString("details");
            } catch (Exception e) {
                // TODO: handle exception
            }
            return otherRule;
        }
        TextContestRule none = new TextContestRule();
        return none;


    }
    
    private   TextContestRule getTitleRecording(String language, int type, String jsonContent) {
        if (type == 1) {
            TextContestRule timeRule = new TextContestRule();
            timeRule.title = "Thời gian";
            timeRule.details = "Bắt đầu lúc 21:00 mỗi ngày, kết thúc 20:59 ngày hôm sau";
            try {
                JSONObject jsonObject = new JSONObject(jsonContent);
                timeRule.title = jsonObject.getString("title");
                timeRule.details = jsonObject.getString("details");
            } catch (Exception e) {
                // TODO: handle exception
            }
            return timeRule;
        }
        
        if (type == 2) {
            TextContestRule mainRule = new TextContestRule();
            mainRule.title = "Quy tắc";
            mainRule.details = 
              "1. Bảng xếp hạng được sắp xếp dựa theo điểm bài thu. Điểm bài thu là tổng điểm quà tặng + điểm lượt nghe, lượt thích, lượt bình luận hợp lệ.\n\n"
            + "2. Điểm quà tặng bằng 10 lần giá trị của quà tặng. Ví dụ quà tặng có giá 1 iCoin tương đương với 10 điểm, quà tặng có giá 8 iCoin tương đương với 80 điểm.\n\n"
            + "3. Điểm cho mỗi lượt nghe, thích, bình luận được tính tùy thuộc vào Level của người đó. Cụ thể:\n"
            + "• Level 1-9 được 50 điểm\n"
            + "• Level 10-19 được 100 điểm\n"
            + "• Level 20-29 được 200 điểm\n"
            + "• Level 30-39 được 300 điểm\n"
            + "(Mỗi người chỉ được tính một lượt nghe, thích, bình luận hợp lệ)";
            try {
                JSONObject jsonObject = new JSONObject(jsonContent);
                mainRule.title = jsonObject.getString("title");
                mainRule.details = jsonObject.getString("details");
            } catch (Exception e) {
                // TODO: handle exception
            }
            return mainRule;
        }
        
        if (type == 3) {
            TextContestRule otherRule = new TextContestRule();
            otherRule.title = "Lưu ý";
            otherRule.details = 
                    " Trong thời gian tham gia, nếu phát hiện người dùng nào sử dụng các thủ đoạn bất thường để đua top, chúng tôi sẽ lập tức hủy quyền dự thi, khóa tài khoản mà không cần thông báo trước!\n"
                    + " Các hành vi vi phạm bao gồm:\n"
                    + "    1. Giả mạo hoặc mượn tài khoản người khác để thi.\n"
                    + "    2. Tạo nhiều tài khoản (cùng một chủ) để dự thi.\n"
                    + "    3. Sử dụng hành vi gian lận điểm số.\n"
                    + "    4. Sử dụng bình luận ác ý với bài hát của người khác.\n"
                    + "Quyền giải thích cuối cùng thuộc về đội ngũ của iKara - Yokara.";
            try {
                JSONObject jsonObject = new JSONObject(jsonContent);
                otherRule.title = jsonObject.getString("title");
                otherRule.details = jsonObject.getString("details");
            } catch (Exception e) {
                // TODO: handle exception
            }
            return otherRule;
        }
        TextContestRule none = new TextContestRule();
        return none;


    }
      
    private  String getIDgiftDefaults(String language, int type) {
        switch (type) {
            case TYPERECORDING:
                return "RecordingDefaults" + language;
            case TYPEKARA:
                return "KaraDefaults" + language;
            case TYPEFAMILY:
                return "FamilyDefaults" + language;
                
        }
        return "";
    }
    
    private  ArrayList<ContestGiftItem> getGiftEvent(String language, int type, int position, Entity entityGift) {
        ArrayList<ContestGiftItem> allGift = new ArrayList<ContestGiftItem>();
        Entity giftEntity = null;

        if (entityGift == null) {
            String idString = getIDgiftDefaults(language, type);
            Key keyGiftKey = KeyFactory.createKey("ContentEvent", idString);
            Entity giftFromMemcache = getEntityFromMemcache(keyGiftKey);
            giftEntity = giftFromMemcache;
        } else {
            giftEntity = entityGift; 
        }
        if (giftEntity == null) return null;
        
        String nameProperty = "gifts_1";
        switch (position) {
            case 2:
                nameProperty = "gifts_2";
                break;
            case 3:
                nameProperty = "gifts_3";
                break;
            case 4:
                nameProperty = "gifts_4_10";
                break;
        }
        //System.out.println(giftEntity.getKey().getName());
        //JSONObject jsonObject = new JSONObject( Utils.getString(giftEntity.getProperty(nameProperty), "{}"));
        ContentEventModel contentEventModel = null;
        
        try {
            String contentString = Utils.getString(giftEntity.getProperty(nameProperty), "");
            if (contentString.isEmpty()) return null;
            contentEventModel = Utils.deserialize(ContentEventModel.class, contentString);

        } catch (Exception e) {
            // TODO: handle exception
        }
        
        if (contentEventModel == null) return null;
        ContestGiftItem yacht = new ContestGiftItem();
        yacht.imageUrl = contentEventModel.cupUrl;
        yacht.text = contentEventModel.message;
        yacht.keyCup = contentEventModel.keyCup;
        
        ContestGiftItem top1Frame = new ContestGiftItem();
        top1Frame.imageUrl = contentEventModel.frameUrl;
        top1Frame.text = contentEventModel.frameMessage;
        top1Frame.keyCup = contentEventModel.frameIdString;
        
        ContestGiftItem systemNotification = new ContestGiftItem();
        systemNotification.imageUrl = contentEventModel.notificationUrl;
        systemNotification.text = contentEventModel.notificationMessage;
        systemNotification.keyCup = contentEventModel.keyCup;
        
//        if (systemNotification.imageUrl == null || systemNotification.imageUrl.isEmpty()) {
//            systemNotification.imageUrl = "https://ikara-data.s3.us-west-002.backblazeb2.com/images/giftitems/systemnotification.png";
//        }
//        if (systemNotification.text == null || systemNotification.text.isEmpty()) {
//            systemNotification.text = "Thông báo toàn hệ thống";
//        }
        
        if (yacht.imageUrl != null || yacht.text != null)
        allGift.add(yacht);
        if (top1Frame.imageUrl != null || top1Frame.text != null)
        allGift.add(top1Frame);
        if (systemNotification.imageUrl != null || systemNotification.text != null)
        allGift.add(systemNotification);
        
        return allGift;
    }

    private  void setEventRecordingIkara(Entity entity) {
            String language = Language.Vietnamese; 
            if (entity == null) {
            ContestRules contestRules = new ContestRules();
            contestRules.textContestRules = new ArrayList<TextContestRule>();

            contestRules.textContestRules.add(getTitleRecording(language, 1, null));
            contestRules.textContestRules.add(getTitleRecording(language, 2, null));
            contestRules.textContestRules.add(getTitleRecording(language, 3, null));
            
            contestRules.gifts_1 = getGiftEvent(language, TYPERECORDING, 1, entity);
            contestRules.gifts_2 = getGiftEvent(language, TYPERECORDING, 2, entity);
            contestRules.gifts_3 = getGiftEvent(language, TYPERECORDING, 3, entity);
            contestRules.gifts_4_10 = getGiftEvent(language, TYPERECORDING, 4, entity);
            
            eventRecordingIkara = contestRules;
            } else {
                ContestRules contestRules = new ContestRules();
                contestRules.textContestRules = new ArrayList<TextContestRule>();

                contestRules.textContestRules.add(getTitleRecording(language, 1, Utils.getString(entity.getProperty("timeRule"), null)));
                contestRules.textContestRules.add(getTitleRecording(language, 2, Utils.getString(entity.getProperty("mainRule"), null)));
                contestRules.textContestRules.add(getTitleRecording(language, 3, Utils.getString(entity.getProperty("otherRule"), null)));
                
                contestRules.gifts_1 = getGiftEvent(language, TYPERECORDING, 1, entity);
                contestRules.gifts_2 = getGiftEvent(language, TYPERECORDING, 2, entity);
                contestRules.gifts_3 = getGiftEvent(language, TYPERECORDING, 3, entity);
                contestRules.gifts_4_10 = getGiftEvent(language, TYPERECORDING, 4, entity);
                
                eventRecordingIkara = contestRules;
            }
    }
    
    private  void setEventRecordingYokara(Entity entity) {
        String language = Language.English; 
        if (entity == null) {
        ContestRules contestRules = new ContestRules();
        contestRules.textContestRules = new ArrayList<TextContestRule>();

        contestRules.textContestRules.add(getTitleRecording(language, 1, null));
        contestRules.textContestRules.add(getTitleRecording(language, 2, null));
        contestRules.textContestRules.add(getTitleRecording(language, 3, null));
        
        contestRules.gifts_1 = getGiftEvent(language, TYPERECORDING, 1, null);
        contestRules.gifts_2 = getGiftEvent(language, TYPERECORDING, 2, null);
        contestRules.gifts_3 = getGiftEvent(language, TYPERECORDING, 3, null);
        contestRules.gifts_4_10 = getGiftEvent(language, TYPERECORDING, 4, null);
        
        eventRecordingYokara = contestRules;
    } else {
        ContestRules contestRules = new ContestRules();
        contestRules.textContestRules = new ArrayList<TextContestRule>();

        contestRules.textContestRules.add(getTitleRecording(language, 1, Utils.getString(entity.getProperty("timeRule"), null)));
        contestRules.textContestRules.add(getTitleRecording(language, 2, Utils.getString(entity.getProperty("mainRule"), null)));
        contestRules.textContestRules.add(getTitleRecording(language, 3, Utils.getString(entity.getProperty("otherRule"), null)));
        
        contestRules.gifts_1 = getGiftEvent(language, TYPERECORDING, 1, entity);
        contestRules.gifts_2 = getGiftEvent(language, TYPERECORDING, 2, entity);
        contestRules.gifts_3 = getGiftEvent(language, TYPERECORDING, 3, entity);
        contestRules.gifts_4_10 = getGiftEvent(language, TYPERECORDING, 4, entity);
        
        eventRecordingYokara = contestRules;
    }
    }

    private  void setEventKaraIkara(Entity entity) {
        String language = Language.Vietnamese; 
        if (entity == null) {
        GetRulesTopLiveRoomsResponse contestRules = new GetRulesTopLiveRoomsResponse();
        contestRules.textContestRules = new ArrayList<TextContestRule>();

        contestRules.textContestRules.add(getTitleKara(language, 1, null));
        contestRules.textContestRules.add(getTitleKara(language, 2, null));
        contestRules.textContestRules.add(getTitleKara(language, 3, null));
        
        contestRules.gifts_1 = getGiftEvent(language, TYPEKARA, 1, null);
        contestRules.gifts_2 = getGiftEvent(language, TYPEKARA, 2, null);
        contestRules.gifts_3 = getGiftEvent(language, TYPEKARA, 3, null);
        
        eventKaraIkara = contestRules;
    } else {
        GetRulesTopLiveRoomsResponse contestRules = new GetRulesTopLiveRoomsResponse();
        contestRules.textContestRules = new ArrayList<TextContestRule>();

        contestRules.textContestRules.add(getTitleRecording(language, 1, Utils.getString(entity.getProperty("timeRule"), null)));
        contestRules.textContestRules.add(getTitleRecording(language, 2, Utils.getString(entity.getProperty("mainRule"), null)));
        contestRules.textContestRules.add(getTitleRecording(language, 3, Utils.getString(entity.getProperty("otherRule"), null)));
        
        contestRules.gifts_1 = getGiftEvent(language, TYPEKARA, 1, entity);
        contestRules.gifts_2 = getGiftEvent(language, TYPEKARA, 2, entity);
        contestRules.gifts_3 = getGiftEvent(language, TYPEKARA, 3, entity);

        
        eventKaraIkara = contestRules;
    }

}

    private  void setEventKaraYokara(Entity entity) {
        String language = Language.English; 
        if (entity == null) {
        GetRulesTopLiveRoomsResponse contestRules = new GetRulesTopLiveRoomsResponse();
        contestRules.textContestRules = new ArrayList<TextContestRule>();

        contestRules.textContestRules.add(getTitleKara(language, 1, null));
        contestRules.textContestRules.add(getTitleKara(language, 2, null));
        contestRules.textContestRules.add(getTitleKara(language, 3, null));
        
        contestRules.gifts_1 = getGiftEvent(language, TYPEKARA, 1, null);
        contestRules.gifts_2 = getGiftEvent(language, TYPEKARA, 2, null);
        contestRules.gifts_3 = getGiftEvent(language, TYPEKARA, 3, null);
        
        eventKaraYokara = contestRules;
    } else {
        GetRulesTopLiveRoomsResponse contestRules = new GetRulesTopLiveRoomsResponse();
        contestRules.textContestRules = new ArrayList<TextContestRule>();

        contestRules.textContestRules.add(getTitleRecording(language, 1, Utils.getString(entity.getProperty("timeRule"), null)));
        contestRules.textContestRules.add(getTitleRecording(language, 2, Utils.getString(entity.getProperty("mainRule"), null)));
        contestRules.textContestRules.add(getTitleRecording(language, 3, Utils.getString(entity.getProperty("otherRule"), null)));
        
        contestRules.gifts_1 = getGiftEvent(language, TYPEKARA, 1, entity);
        contestRules.gifts_2 = getGiftEvent(language, TYPEKARA, 2, entity);
        contestRules.gifts_3 = getGiftEvent(language, TYPEKARA, 3, entity);
        
        eventKaraYokara = contestRules;
    }
}
    
    private  void setEventFamilyIkara(Entity entity) {
        String language = Language.Vietnamese; 
        if (entity == null) {
        GetRulesFamilyResponse contestRules = new GetRulesFamilyResponse();
        contestRules.textContestRules = new ArrayList<TextContestRule>();

        contestRules.textContestRules.add(getTitleFamily(language, 1, null));
        contestRules.textContestRules.add(getTitleFamily(language, 2, null));
        contestRules.textContestRules.add(getTitleFamily(language, 3, null));
        
        contestRules.gifts_1 = getGiftEvent(language, TYPEFAMILY, 1, null);
        contestRules.gifts_2 = getGiftEvent(language, TYPEFAMILY, 2, null);
        contestRules.gifts_3 = getGiftEvent(language, TYPEFAMILY, 3, null);

        eventFamilyIkara = contestRules;
        } else {
            GetRulesFamilyResponse contestRules = new GetRulesFamilyResponse();
            contestRules.textContestRules = new ArrayList<TextContestRule>();

            contestRules.textContestRules.add(getTitleRecording(language, 1, Utils.getString(entity.getProperty("timeRule"), null)));
            contestRules.textContestRules.add(getTitleRecording(language, 2, Utils.getString(entity.getProperty("mainRule"), null)));
            contestRules.textContestRules.add(getTitleRecording(language, 3, Utils.getString(entity.getProperty("otherRule"), null)));
            
            contestRules.gifts_1 = getGiftEvent(language, TYPEFAMILY, 1, entity);
            contestRules.gifts_2 = getGiftEvent(language, TYPEFAMILY, 2, entity);
            contestRules.gifts_3 = getGiftEvent(language, TYPEFAMILY, 3, entity);
            
            eventFamilyIkara = contestRules;
        }
}
    
    private  void setEventFamilyYokara(Entity entity) {
        String language = Language.English; 
        if (entity == null) {
        GetRulesFamilyResponse contestRules = new GetRulesFamilyResponse();
        contestRules.textContestRules = new ArrayList<TextContestRule>();

        contestRules.textContestRules.add(getTitleFamily(language, 1, null));
        contestRules.textContestRules.add(getTitleFamily(language, 2, null));
        contestRules.textContestRules.add(getTitleFamily(language, 3, null));
        
        contestRules.gifts_1 = getGiftEvent(language, TYPEFAMILY, 1, null);
        contestRules.gifts_2 = getGiftEvent(language, TYPEFAMILY, 2, null);
        contestRules.gifts_3 = getGiftEvent(language, TYPEFAMILY, 3, null);

        eventFamilyYokara = contestRules;
        } else {
            GetRulesFamilyResponse contestRules = new GetRulesFamilyResponse();
            contestRules.textContestRules = new ArrayList<TextContestRule>();

            contestRules.textContestRules.add(getTitleRecording(language, 1, Utils.getString(entity.getProperty("timeRule"), null)));
            contestRules.textContestRules.add(getTitleRecording(language, 2, Utils.getString(entity.getProperty("mainRule"), null)));
            contestRules.textContestRules.add(getTitleRecording(language, 3, Utils.getString(entity.getProperty("otherRule"), null)));
            
            contestRules.gifts_1 = getGiftEvent(language, TYPEFAMILY, 1, entity);
            contestRules.gifts_2 = getGiftEvent(language, TYPEFAMILY, 2, entity);
            contestRules.gifts_3 = getGiftEvent(language, TYPEFAMILY, 3, entity);
            
            eventFamilyYokara = contestRules;
        }
    }
    
    private  String getJsonCupRank(Key key) {
        String language = Language.Vietnamese;
        int typeString = TYPERECORDING;
        ContentEventModel contentEventModel = new ContentEventModel();
        
        Entity entityGiftEntity;
        try {
            Entity giftFromMemcache = getEntityFromMemcache(key);
            entityGiftEntity = giftFromMemcache;

            

            
            contentEventModel.notificationUrl = Utils.getString(entityGiftEntity.getProperty("notificationUrl"), null);
            contentEventModel.keyCup = Utils.getString(entityGiftEntity.getProperty("keyCup"), null);
            contentEventModel.cupTop = Utils.getString(entityGiftEntity.getProperty("cupTop"), null);
            contentEventModel.cupUrl = Utils.getString(entityGiftEntity.getProperty("cupUrl"), null);
            contentEventModel.notificationMessage = Utils.getString(entityGiftEntity.getProperty("notificationMessage"), null);
            contentEventModel.frameMessage = Utils.getString(entityGiftEntity.getProperty("frameMessage"), null);
            contentEventModel.language = Utils.getString(entityGiftEntity.getProperty("language"), null);
            contentEventModel.message = Utils.getString(entityGiftEntity.getProperty("message"), null);
            contentEventModel.frameUrl = Utils.getString(entityGiftEntity.getProperty("frameUrl"), null);
            contentEventModel.cupUrlForEvent = Utils.getString(entityGiftEntity.getProperty("cupUrlForEvent"), null);
            contentEventModel.lastUpdate = Utils.getString(entityGiftEntity.getProperty("lastUpdate"), null);
            contentEventModel.keyCupForEvent = Utils.getString(entityGiftEntity.getProperty("keyCupForEvent"), null);
            contentEventModel.cupFor = Utils.getString(entityGiftEntity.getProperty("cupFor"), null);
            
            
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return Utils.serialize(contentEventModel);
        
    }
    
    private  void updateEventShow(Entity entity) {
        updateEventActive(entity);
    }
    
    private  void updateEventActive(Entity entity) {
        String nameString = entity.getKey().getName();
        if (nameString.endsWith(Language.Vietnamese)) {
            if (nameString.startsWith("Recording")) {
                setEventRecordingIkara(entity);
            } else if (nameString.startsWith("Kara")) {
                setEventKaraIkara(entity);
            } else if (nameString.startsWith("Family")) {
                setEventFamilyIkara(entity); 
            }
        }
        
        if (nameString.endsWith(Language.English)) {
            if (nameString.startsWith("Recording")) {
                setEventRecordingYokara(entity);
            } else if (nameString.startsWith("Kara")) {
                setEventKaraYokara(entity);
            } else if (nameString.startsWith("Family")) {
                setEventFamilyYokara(entity);
            }
        } 
        
        if (nameString.endsWith("all")) {
            if (nameString.startsWith("Recording")) {
                setEventRecordingYokara(entity);
                setEventRecordingIkara(entity);
            } else if (nameString.startsWith("Kara")) {
                setEventKaraYokara(entity);
                setEventKaraIkara(entity);
            } else if (nameString.startsWith("Family")) {
                setEventFamilyYokara(entity);
                setEventFamilyIkara(entity);
            }
        } 

    }
    
    public List<Entity> loadDBtoMemcache() {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query query = new Query("ContentEvent");
        PreparedQuery pq2 = datastore.prepare(query);
        FetchOptions fetchOptions = FetchOptions.Builder.withDefaults();
        List<Entity> results = pq2.asList(fetchOptions);
        saveMemcache(results);
        return results;
}
    
    private void loadMemcache() {
            MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
            syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
            try {
                 memcacheFullEvent = (List<Entity>) syncCache.get(keyMemcacheString); // read from cache
            } catch (Exception e) {
                memcacheFullEvent = null;
            }
    }
    
    private Entity getEntityFromMemcache(Key keyEvent) {
        if (memcacheFullEvent == null || keyEvent == null) return null;
        for (Entity entity : memcacheFullEvent) {
            if (entity.getKey().equals(keyEvent)) {
                return entity;
            }
        }
        return null;
    }
    
    private void saveMemcache(List<Entity> resultsList) {
        MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        Date currentDate = new Date();
        Date tomorrow = new Date(currentDate.getYear(), currentDate.getMonth(), currentDate.getDate(),
                Utils.getCurrentTimezoneOffset() + 14, 0, 0);
        if (currentDate.getTime() - tomorrow.getTime() >= 0) {
            tomorrow.setDate(tomorrow.getDate() + 1);
        }
        syncCache.put(keyMemcacheString, resultsList, Expiration.onDate(tomorrow)); // populate cache
    }
    
    
}
