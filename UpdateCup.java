package vnapps.ikara.servlet.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.appengine.tools.remoteapi.RemoteApiOptions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.googlecode.objectify.remotely.RemoteCheck;
import com.googlecode.objectify.remotely.Remotely;

import vnapps.ikara.common.v19.DigitalSignature;
import vnapps.ikara.common.v35.Utils;
import vnapps.ikara.restful.common.Response;
import vnapps.ikara.servlet.webModel.CupFamilyModel;
import vnapps.ikara.servlet.webModel.CupFamilyModelResponse;
import vnapps.ikara.servlet.webModel.CupLiveRoomModel;
import vnapps.ikara.servlet.webModel.CupLiveRoomModelResponse;
import vnapps.ikara.servlet.webModel.CupModel;
import vnapps.ikara.servlet.webModel.CupModelResponse;
import vnapps.ikara.servlet.webModel.CupRecordingHotModel;
import vnapps.ikara.servlet.webModel.CupRecordingHotModelResponse;
import vnapps.ikara.servlet.webModel.CupRequest;
import vnapps.ikara.servlet.webModel.CupResponse;
import vnapps.ikara.servlet.webModel.EventNameResponse;

@WebServlet("/cup/getCups")
public class UpdateCup extends HttpServlet {

	private static final Logger log = Logger.getLogger(UpdateCup.class.getName());
	private static final String RECORDINGHOT = "BAITHUHOT";
	private static final String LIVEROOM = "PHONGKARA";
	private static final String FAMILY = "GIATOC";
	private static final String RECORDING = "Recording";
	private static final String KARA = "Kara";
	private static final String FAMILYDB = "Family";
	private static final String IDNORMAL = "4509053559898112,4942244913479680,4635071067062272,6020845553451008,5718674135973888,6687107656843264,5777082939736064,6448951720280064,4640350454284288,6128978200231936";
	private static final String IDNAMEDEFAULT = "FamilyDefaultsen.yokara,FamilyDefaultsvi,KaraDefaultsen.yokara,KaraDefaultsvi,RecordingDefaultsen.yokara,RecordingDefaultsvi";

	static DatastoreService datastore = null;
	
	static {
        datastore = DatastoreServiceFactory.getDatastoreService();
        if (Utils.getApplicationId().equals("no_app_id")) {
            Utils.setEnv("GOOGLE_APPLICATION_CREDENTIALS", "/Volumes/DATA/ikara4m.json");

            RemoteApiOptions options = new RemoteApiOptions().server("ikara4m.appspot.com", 443)
                    .remoteApiPath("/remote-api").useServiceAccountCredential("ikara4m@appspot.gserviceaccount.com",
                            "/Users//tranhongminh//inmobi//notes//ikara4m-1d7122c21b97.p12");

            RemoteCheck check = new RemoteCheck() {
                public boolean isRemote(String namespace) {
                    return true;
                }
            };
            Remotely remotely = new Remotely(options, check);
            datastore = remotely.intercept(datastore);
        }
    }

	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String parametersEncode = req.getParameter("parameters");
        try {
            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            String parametersInString = DigitalSignature.decryption(req.getParameter("parameters"));
            resp.setContentType("text/plain;charset=UTF-8");
            CupRequest parameters = Utils.deserialize(CupRequest.class, parametersInString);
//            CupResponse cupResponse = getCup(parameters);
            CupModelResponse cupResponse = getCupsWithDate(parameters);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write(Utils.serialize(cupResponse));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @POST
////    @Path("/getCups")
//    @Consumes({ MediaType.APPLICATION_JSON })
//    @Produces({ MediaType.APPLICATION_JSON })
    public static CupResponse getCups(CupRequest request) {
        CupResponse response = new CupResponse();
        response = getListCupForEvent();
        if(request.cupFor != null && request.cupFor.contains(",")) {
            // init
            String[] cupForArr = request.cupFor.split(",");
            for (String cupFor : cupForArr) {
                if(RECORDINGHOT.equals(cupFor.trim())) {
                    response.cupRecordingHotModels = new ArrayList<>();
//                    response.cupRecordingHotForEvents = new ArrayList<>();
                    Query query = new Query("CupRank").addFilter("cupFor", FilterOperator.EQUAL, RECORDINGHOT);
                    //.addSort("cupTop", SortDirection.ASCENDING);
                    PreparedQuery pq = datastore.prepare(query);
                    List<Entity> results = pq.asList(FetchOptions.Builder.withDefaults());
                    List<Long> listCupTop = new ArrayList<>();
                    for (Entity entity : results) {
                        listCupTop.add((long) entity.getProperty("cupTop"));
                    }
                    listCupTop.sort(Comparator.naturalOrder());
                    Set<Long> targetSet = new HashSet<>(listCupTop);
                    List<Long> targetList = new ArrayList<>(targetSet);
                    for (Long long1 : targetList) {
                        for (Entity entity : results) {
                            if(IDNORMAL.contains(Long.toString(entity.getKey().getId()))
                                    && long1 == (long) entity.getProperty("cupTop")) {
                                CupRecordingHotModel cupRecordingHotModel = new CupRecordingHotModel();
                                cupRecordingHotModel.cupTop = getCupTypeString((long) entity.getProperty("cupTop"));
                                cupRecordingHotModel.cupUrl = (String) entity.getProperty("cupUrl");
                                cupRecordingHotModel.keyCup = (String) entity.getProperty("keyCup");
                                response.cupRecordingHotModels.add(cupRecordingHotModel);
                            } else if(long1 == (long) entity.getProperty("cupTop")) {
                                CupRecordingHotModel cupRecordingHotModel = new CupRecordingHotModel();
                                cupRecordingHotModel.cupTop = getCupTypeString((long) entity.getProperty("cupTop"));
                                cupRecordingHotModel.keyCupForEvent = (String) entity.getProperty("keyCup");
                                cupRecordingHotModel.cupUrlForEvent = (String) entity.getProperty("cupUrl");
//                                response.cupRecordingHotForEvents.add(cupRecordingHotModel);
                                
                            }
                        }
                        response.status = "OK";
                    }
                } else if(LIVEROOM.equals(cupFor.trim())) {
                    response.cupLiveRoomModels = new ArrayList<>();
//                    response.cupLiveRoomForEvents = new ArrayList<>();
                    Query query = new Query("CupRank").addFilter("cupFor", FilterOperator.EQUAL, LIVEROOM);
                    PreparedQuery pq = datastore.prepare(query);
                    List<Entity> results = pq.asList(FetchOptions.Builder.withDefaults());
                    List<Long> listCupTop = new ArrayList<>();
                    for (Entity entity : results) {
                        listCupTop.add((long) entity.getProperty("cupTop"));
                    }
                    listCupTop.sort(Comparator.naturalOrder());
                    Set<Long> targetSet = new HashSet<>(listCupTop);
                    List<Long> targetList = new ArrayList<>(targetSet);
                    for (Long cupTop : targetList) {
                        for (Entity entity : results) {
                            if(IDNORMAL.contains(Long.toString(entity.getKey().getId()))
                                    && cupTop == (long) entity.getProperty("cupTop") && "vi".equals(entity.getProperty("language"))) {
                                CupLiveRoomModel cupLiveRoomModel = new CupLiveRoomModel();
                                cupLiveRoomModel.cupTop = getCupTypeString((long) entity.getProperty("cupTop"));
                                cupLiveRoomModel.cupUrl = (String) entity.getProperty("cupUrl");
                                cupLiveRoomModel.keyCup = (String) entity.getProperty("keyCup");
                                response.cupLiveRoomModels.add(cupLiveRoomModel);
                            } else if(cupTop == (long) entity.getProperty("cupTop") && "vi".equals(entity.getProperty("language"))) {
                                CupLiveRoomModel cupLiveRoomModel = new CupLiveRoomModel();
                                cupLiveRoomModel.cupTop = getCupTypeString((long) entity.getProperty("cupTop"));
                                cupLiveRoomModel.keyCupForEvent = (String) entity.getProperty("keyCup");
                                cupLiveRoomModel.cupUrlForEvent = (String) entity.getProperty("cupUrl");
//                                response.cupLiveRoomForEvents.add(cupLiveRoomModel);
                            }
                        }
                        response.status = "OK";
                    }
                } else if(FAMILY.equals(cupFor.trim())) {
                    response.cupFamilyModels = new ArrayList<>();
                    Query query = new Query("CupRank").addFilter("cupFor", FilterOperator.EQUAL, FAMILY);
                    PreparedQuery pq = datastore.prepare(query);
                    List<Entity> results = pq.asList(FetchOptions.Builder.withDefaults());
                    List<Long> listCupTop = new ArrayList<>();
                    for (Entity entity : results) {
                        listCupTop.add((long) entity.getProperty("cupTop"));
                    }
                    listCupTop.sort(Comparator.naturalOrder());
                    for (Long long1 : listCupTop) {
                        for (Entity entity : results) {
                            if(long1 == (long) entity.getProperty("cupTop")) {
                                CupFamilyModel cupFamilyModel = new CupFamilyModel();
                                cupFamilyModel.cupTop = getCupTypeString((long) entity.getProperty("cupTop"));
                                cupFamilyModel.cupUrl = (String) entity.getProperty("cupUrl");
                                cupFamilyModel.keyCup = (String) entity.getProperty("keyCup");
                                response.cupFamilyModels.add(cupFamilyModel);
                            }
                        }
                        response.status = "OK";
                    }
                }
            }
        } else if(request.keyCup != null && request.imgUrl != null) {
            // update cup
            if(request.keyCupNew != null && request.imgUrlNew != null) {
                Query query = new Query("CupRank").addFilter("keyCup", FilterOperator.EQUAL, request.keyCup);
//                        .addFilter("cupUrl", FilterOperator.EQUAL, request.imgUrl)
//                        .addFilter("cupTop", FilterOperator.EQUAL, request.cupTop);
                PreparedQuery pq = datastore.prepare(query);
                List<Entity> results = pq.asList(FetchOptions.Builder.withDefaults());
                long cupTopRq = getCupTypeInt(request.cupTop);
                for (Entity cupRank : results) {
                    long cupTopDb = (long) cupRank.getProperty("cupTop");
                    if(request.imgUrl.equals(cupRank.getProperty("cupUrl")) && cupTopDb == cupTopRq) {
                        cupRank.setProperty("keyCup", request.keyCupNew);
                        cupRank.setProperty("cupUrl", request.imgUrlNew);
                        datastore.put(cupRank);
                        response.status = "OK";
                        response.message = "Update thành công!";
                        break;
                    }
                }
//                Entity cupRank = pq.asSingleEntity();
            } else {
                response.status = "NG";
                response.message = "Update thất bại! (Bạn chưa chọn CUP mới)";
            }
        } else if(request.keyCupForEvent != null && request.cupUrlForEvent != null) {
            // Su Kien: update cup
            if(request.keyCupForEventNew != null && request.cupUrlForEventNew != null) {
                Query query = new Query("CupRank").addFilter("keyCup", FilterOperator.EQUAL, request.keyCupForEvent);
                PreparedQuery pq = datastore.prepare(query);
//                Entity cupRank = pq.asSingleEntity();
                List<Entity> results = pq.asList(FetchOptions.Builder.withDefaults());
                long cupTopRq = getCupTypeInt(request.cupTop);
                for (Entity cupRank : results) {
                    long cupTopDb = (long) cupRank.getProperty("cupTop");
                    if(request.cupUrlForEvent.equals(cupRank.getProperty("cupUrl"))
                            && request.keyCupForEvent.equals(cupRank.getProperty("keyCup"))
                            && request.cupFor.equals(cupRank.getProperty("cupFor"))
                            && cupTopDb == cupTopRq) {
                        if(request.displayCupStart != null && request.displayCupEnd != null
                                && request.playingStart != null && request.playingEnd != null) {
                            cupRank.setProperty("keyCup", request.keyCupForEventNew);
                            cupRank.setProperty("cupUrl", request.cupUrlForEventNew);

                            cupRank.setProperty("displayCupStart", request.displayCupStart);
                            cupRank.setProperty("displayCupEnd", request.displayCupEnd);
                            cupRank.setProperty("playingStart", request.playingStart);
                            cupRank.setProperty("playingEnd", request.playingEnd);
                            datastore.put(cupRank);
                            response.status = "OK";
                            response.message = "Set thành công!";
                            break;
                        } else {
                            response.status = "NG";
                            response.message = "Set thất bại! (Bạn chưa chọn ngày giờ)";
                        }
                    }
                }
            } else {
                response.status = "NG";
                response.message = "Set thất bại! (Bạn chưa chọn CUP mới)";
            }
        }
        return response;
    }
    private static String getCupTypeString(long cupTop) {
        if(cupTop == 1) {
            return "TOP 1";
        } else if(cupTop == 2) {
            return "TOP 2";
        } else if(cupTop == 3) {
            return "TOP 3";
        } else {
            return "TOP 4-10";
        }
    }
    private static long getCupTypeInt(String cupTop) {
        if("TOP 1".equals(cupTop)) {
            return 1;
        } else if("TOP 2".equals(cupTop)) {
            return 2;
        } else if("TOP 3".equals(cupTop)) {
            return 3;
        } else {
            return 4;
        }
    }
    private static CupResponse getListCupForEvent() {
        CupResponse response = new CupResponse();
        List<CupModel> cupModels = new ArrayList<CupModel>();
        Query query = new Query("ContentEvent");
        PreparedQuery pq = datastore.prepare(query);
//        Entity cupRank = pq.asSingleEntity();
        List<Entity> results = pq.asList(FetchOptions.Builder.withDefaults());
        CupRecordingHotModel cupRecordingHotModel = null;
        CupLiveRoomModel cupLiveRoomModel = null;
        CupFamilyModel cupFamilyModel = null;
        response.eventRecordingHotNames = new ArrayList<>();
        response.eventLiveRoomNames = new ArrayList<>();
        response.eventFamilyNames = new ArrayList<>();
        response.cupRecordingHotForEvents = new ArrayList<>();
        response.cupLiveRoomForEvents = new ArrayList<>();
        response.cupFamilyForEvents = new ArrayList<>();
        for (Entity cup : results) {
            String keyName = cup.getKey().getName();
            // xac dinh cac record event
            if(!IDNAMEDEFAULT.contains(keyName)) {
                // neu la event bai thu hot VI(Recording) Recording20102022vi   Recording20102022en.yokara
                if(keyName.contains(RECORDING)) {
                    String eventName = (String) cup.getProperty("eventName");
                    response.eventRecordingHotNames.add(eventName);
                    JSONObject objectGifts1 = new JSONObject((String) cup.getProperty("gifts_1"));
                    JSONObject objectGifts2 = new JSONObject((String) cup.getProperty("gifts_2"));
                    JSONObject objectGifts3 = new JSONObject((String) cup.getProperty("gifts_3"));
                    if(objectGifts1.has("keyCup")) {
                        cupRecordingHotModel = new CupRecordingHotModel();
                        cupRecordingHotModel.eventName = eventName;
                        cupRecordingHotModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts1.get("cupTop")));
                        cupRecordingHotModel.keyCup = (String) objectGifts1.get("keyCup");
                        cupRecordingHotModel.cupUrl = (String) objectGifts1.get("cupUrl");
                        if(objectGifts1.has("frameUrl")) {
                            cupRecordingHotModel.frameUrl = (String) objectGifts1.get("frameUrl");
                        }
                        response.cupRecordingHotForEvents.add(cupRecordingHotModel);
                    }
                    if(objectGifts2.has("keyCup")) {
                        cupRecordingHotModel = new CupRecordingHotModel();
                        cupRecordingHotModel.eventName = eventName;
                        cupRecordingHotModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts2.get("cupTop")));
                        cupRecordingHotModel.keyCup = (String) objectGifts2.get("keyCup");
                        cupRecordingHotModel.cupUrl = (String) objectGifts2.get("cupUrl");
                        if(objectGifts2.has("frameUrl")) {
                            cupRecordingHotModel.frameUrl = (String) objectGifts2.get("frameUrl");
                        }
                        response.cupRecordingHotForEvents.add(cupRecordingHotModel);
                    }
                    if(objectGifts3.has("keyCup")) {
                        cupRecordingHotModel = new CupRecordingHotModel();
                        cupRecordingHotModel.eventName = eventName;
                        cupRecordingHotModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts3.get("cupTop")));
                        cupRecordingHotModel.keyCup = (String) objectGifts3.get("keyCup");
                        cupRecordingHotModel.cupUrl = (String) objectGifts3.get("cupUrl");
                        if(objectGifts3.has("frameUrl")) {
                            cupRecordingHotModel.frameUrl = (String) objectGifts3.get("frameUrl");
                        }
                        response.cupRecordingHotForEvents.add(cupRecordingHotModel);
                    }
                } else if(keyName.contains(KARA)) {
                    // neu la event Kara
                    String eventName = (String) cup.getProperty("eventName");
                    response.eventLiveRoomNames.add(eventName);
                    JSONObject objectGifts1 = new JSONObject((String) cup.getProperty("gifts_1"));
                    JSONObject objectGifts2 = new JSONObject((String) cup.getProperty("gifts_2"));
                    JSONObject objectGifts3 = new JSONObject((String) cup.getProperty("gifts_3"));
                    if(objectGifts1.has("keyCup")) {
                        cupLiveRoomModel = new CupLiveRoomModel();
                        cupLiveRoomModel.eventName = eventName;
                        cupLiveRoomModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts1.get("cupTop")));
                        cupLiveRoomModel.keyCup = (String) objectGifts1.get("keyCup");
                        cupLiveRoomModel.cupUrl = (String) objectGifts1.get("cupUrl");
                        if(objectGifts1.has("frameUrl")) {
                            cupLiveRoomModel.frameUrl = (String) objectGifts1.get("frameUrl");
                        }
                        response.cupLiveRoomForEvents.add(cupLiveRoomModel);
                    }
                    if(objectGifts2.has("keyCup")) {
                        cupLiveRoomModel = new CupLiveRoomModel();
                        cupLiveRoomModel.eventName = eventName;
                        cupLiveRoomModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts2.get("cupTop")));
                        cupLiveRoomModel.keyCup = (String) objectGifts2.get("keyCup");
                        cupLiveRoomModel.cupUrl = (String) objectGifts2.get("cupUrl");
                        if(objectGifts2.has("frameUrl")) {
                            cupLiveRoomModel.frameUrl = (String) objectGifts2.get("frameUrl");
                        }
                        response.cupLiveRoomForEvents.add(cupLiveRoomModel);
                    }
                    if(objectGifts3.has("keyCup")) {
                        cupLiveRoomModel = new CupLiveRoomModel();
                        cupLiveRoomModel.eventName = eventName;
                        cupLiveRoomModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts3.get("cupTop")));
                        cupLiveRoomModel.keyCup = (String) objectGifts3.get("keyCup");
                        cupLiveRoomModel.cupUrl = (String) objectGifts3.get("cupUrl");
                        if(objectGifts3.has("frameUrl")) {
                            cupLiveRoomModel.frameUrl = (String) objectGifts3.get("frameUrl");
                        }
                        response.cupLiveRoomForEvents.add(cupLiveRoomModel);
                    }
                } else if(keyName.contains(FAMILYDB)) {
                    // neu la event Family
                    String eventName = (String) cup.getProperty("eventName");
                    response.eventFamilyNames.add(eventName);
                    JSONObject objectGifts1 = new JSONObject((String) cup.getProperty("gifts_1"));
                    JSONObject objectGifts2 = new JSONObject((String) cup.getProperty("gifts_2"));
                    JSONObject objectGifts3 = new JSONObject((String) cup.getProperty("gifts_3"));
                    if(objectGifts1.has("keyCup")) {
                        cupFamilyModel = new CupFamilyModel();
                        cupFamilyModel.eventName = eventName;
                        cupFamilyModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts1.get("cupTop")));
                        cupFamilyModel.keyCup = (String) objectGifts1.get("keyCup");
                        cupFamilyModel.cupUrl = (String) objectGifts1.get("cupUrl");
                        if(objectGifts1.has("frameUrl")) {
                            cupFamilyModel.frameUrl = (String) objectGifts1.get("frameUrl");
                        }
                        response.cupFamilyForEvents.add(cupFamilyModel);
                    }
                    if(objectGifts2.has("keyCup")) {
                        cupFamilyModel = new CupFamilyModel();
                        cupFamilyModel.eventName = eventName;
                        cupFamilyModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts2.get("cupTop")));
                        cupFamilyModel.keyCup = (String) objectGifts2.get("keyCup");
                        cupFamilyModel.cupUrl = (String) objectGifts2.get("cupUrl");
                        if(objectGifts2.has("frameUrl")) {
                            cupFamilyModel.frameUrl = (String) objectGifts2.get("frameUrl");
                        }
                        response.cupFamilyForEvents.add(cupFamilyModel);
                    }
                    if(objectGifts3.has("keyCup")) {
                        cupFamilyModel = new CupFamilyModel();
                        cupFamilyModel.eventName = eventName;
                        cupFamilyModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts3.get("cupTop")));
                        cupFamilyModel.keyCup = (String) objectGifts3.get("keyCup");
                        cupFamilyModel.cupUrl = (String) objectGifts3.get("cupUrl");
                        if(objectGifts3.has("frameUrl")) {
                            cupFamilyModel.frameUrl = (String) objectGifts3.get("frameUrl");
                        }
                        response.cupFamilyForEvents.add(cupFamilyModel);
                    }
                }
            }
        }
        return response;
    }
    
    private static CupModelResponse getListCupForEventWithDate() {
        CupModelResponse response = new CupModelResponse();
        Query query = new Query("ContentEvent");
        PreparedQuery pq = datastore.prepare(query);
//        Entity cupRank = pq.asSingleEntity();
        List<Entity> results = pq.asList(FetchOptions.Builder.withDefaults());
        CupRecordingHotModelResponse cupRecordingHotModel = null;
        CupLiveRoomModelResponse cupLiveRoomModel = null;
        CupFamilyModelResponse cupFamilyModel = null;
        response.eventRecordingHotNames = new ArrayList<>();
        response.eventLiveRoomNames = new ArrayList<>();
        response.eventFamilyNames = new ArrayList<>();
        response.cupRecordingHotForEvents = new ArrayList<>();
        response.cupLiveRoomForEvents = new ArrayList<>();
        response.cupFamilyForEvents = new ArrayList<>();
        for (Entity cup : results) {
            String keyName = cup.getKey().getName();
            // xac dinh cac record event
            if(!IDNAMEDEFAULT.contains(keyName)) {
                // neu la event bai thu hot VI(Recording) Recording20102022vi   Recording20102022en.yokara
                if(keyName.contains(RECORDING)) {
                    String eventName = (String) cup.getProperty("eventName");
                    EventNameResponse eventNameResponse = new EventNameResponse();
                    eventNameResponse.keyName = keyName;
                    eventNameResponse.name = eventName;
                    response.eventRecordingHotNames.add(eventNameResponse);
                    Date startDateObject = Utils.getDate(cup.getProperty("timeShowStart"), new Date(0));
                    Date endDateObject = Utils.getDate(cup.getProperty("timeShowEnd"),new Date(0));
                    JSONObject objectGifts1 = new JSONObject((String) cup.getProperty("gifts_1"));
                    JSONObject objectGifts2 = new JSONObject((String) cup.getProperty("gifts_2"));
                    JSONObject objectGifts3 = new JSONObject((String) cup.getProperty("gifts_3"));
                    if(objectGifts1.has("keyCup")) {
                        cupRecordingHotModel = new CupRecordingHotModelResponse();
                        cupRecordingHotModel.eventName = eventName;
                        cupRecordingHotModel.timeShowStart = startDateObject;
                        cupRecordingHotModel.timeShowEnd = endDateObject;
                        cupRecordingHotModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts1.get("cupTop")));
                        cupRecordingHotModel.keyCup = (String) objectGifts1.get("keyCup");
                        cupRecordingHotModel.cupUrl = (String) objectGifts1.get("cupUrl");
                        if(objectGifts1.has("frameUrl")) {
                            cupRecordingHotModel.frameUrl = (String) objectGifts1.get("frameUrl");
                        }
                        response.cupRecordingHotForEvents.add(cupRecordingHotModel);
                    }
                    if(objectGifts2.has("keyCup")) {
                        cupRecordingHotModel = new CupRecordingHotModelResponse();
                        cupRecordingHotModel.eventName = eventName;
                        cupRecordingHotModel.eventName = eventName;
                        cupRecordingHotModel.timeShowStart = startDateObject;
                        cupRecordingHotModel.timeShowEnd = endDateObject;
                        cupRecordingHotModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts2.get("cupTop")));
                        cupRecordingHotModel.keyCup = (String) objectGifts2.get("keyCup");
                        cupRecordingHotModel.cupUrl = (String) objectGifts2.get("cupUrl");
                        if(objectGifts2.has("frameUrl")) {
                            cupRecordingHotModel.frameUrl = (String) objectGifts2.get("frameUrl");
                        }
                        response.cupRecordingHotForEvents.add(cupRecordingHotModel);
                    }
                    if(objectGifts3.has("keyCup")) {
                        cupRecordingHotModel = new CupRecordingHotModelResponse();
                        cupRecordingHotModel.eventName = eventName;
                        cupRecordingHotModel.timeShowStart = startDateObject;
                        cupRecordingHotModel.timeShowEnd = endDateObject;
                        cupRecordingHotModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts3.get("cupTop")));
                        cupRecordingHotModel.keyCup = (String) objectGifts3.get("keyCup");
                        cupRecordingHotModel.cupUrl = (String) objectGifts3.get("cupUrl");
                        if(objectGifts3.has("frameUrl")) {
                            cupRecordingHotModel.frameUrl = (String) objectGifts3.get("frameUrl");
                        }
                        response.cupRecordingHotForEvents.add(cupRecordingHotModel);
                    }
                } else if(keyName.contains(KARA)) {
                    // neu la event Kara
                    String eventName = (String) cup.getProperty("eventName");
                    EventNameResponse eventNameResponse = new EventNameResponse();
                    eventNameResponse.keyName = keyName;
                    eventNameResponse.name = eventName;
                    response.eventLiveRoomNames.add(eventNameResponse);
                    Date startDateObject = Utils.getDate(cup.getProperty("timeShowStart"), new Date(0));
                    Date endDateObject = Utils.getDate(cup.getProperty("timeShowEnd"),new Date(0));
                    JSONObject objectGifts1 = new JSONObject((String) cup.getProperty("gifts_1"));
                    JSONObject objectGifts2 = new JSONObject((String) cup.getProperty("gifts_2"));
                    JSONObject objectGifts3 = new JSONObject((String) cup.getProperty("gifts_3"));
                    if(objectGifts1.has("keyCup")) {
                        cupLiveRoomModel = new CupLiveRoomModelResponse();
                        cupLiveRoomModel.eventName = eventName;
                        cupLiveRoomModel.timeShowStart = startDateObject;
                        cupLiveRoomModel.timeShowEnd = endDateObject;
                        cupLiveRoomModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts1.get("cupTop")));
                        cupLiveRoomModel.keyCup = (String) objectGifts1.get("keyCup");
                        cupLiveRoomModel.cupUrl = (String) objectGifts1.get("cupUrl");
                        if(objectGifts1.has("frameUrl")) {
                            cupLiveRoomModel.frameUrl = (String) objectGifts1.get("frameUrl");
                        }
                        response.cupLiveRoomForEvents.add(cupLiveRoomModel);
                    }
                    if(objectGifts2.has("keyCup")) {
                        cupLiveRoomModel = new CupLiveRoomModelResponse();
                        cupLiveRoomModel.eventName = eventName;
                        cupLiveRoomModel.timeShowStart = startDateObject;
                        cupLiveRoomModel.timeShowEnd = endDateObject;
                        cupLiveRoomModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts2.get("cupTop")));
                        cupLiveRoomModel.keyCup = (String) objectGifts2.get("keyCup");
                        cupLiveRoomModel.cupUrl = (String) objectGifts2.get("cupUrl");
                        if(objectGifts2.has("frameUrl")) {
                            cupLiveRoomModel.frameUrl = (String) objectGifts2.get("frameUrl");
                        }
                        response.cupLiveRoomForEvents.add(cupLiveRoomModel);
                    }
                    if(objectGifts3.has("keyCup")) {
                        cupLiveRoomModel = new CupLiveRoomModelResponse();
                        cupLiveRoomModel.eventName = eventName;
                        cupLiveRoomModel.timeShowStart = startDateObject;
                        cupLiveRoomModel.timeShowEnd = endDateObject;
                        cupLiveRoomModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts3.get("cupTop")));
                        cupLiveRoomModel.keyCup = (String) objectGifts3.get("keyCup");
                        cupLiveRoomModel.cupUrl = (String) objectGifts3.get("cupUrl");
                        if(objectGifts3.has("frameUrl")) {
                            cupLiveRoomModel.frameUrl = (String) objectGifts3.get("frameUrl");
                        }
                        response.cupLiveRoomForEvents.add(cupLiveRoomModel);
                    }
                } else if(keyName.contains(FAMILYDB)) {
                    // neu la event Family
                    String eventName = (String) cup.getProperty("eventName");
                    EventNameResponse eventNameResponse = new EventNameResponse();
                    eventNameResponse.keyName = keyName;
                    eventNameResponse.name = eventName;
                    response.eventFamilyNames.add(eventNameResponse);
                    Date startDateObject = Utils.getDate(cup.getProperty("timeShowStart"), new Date(0));
                    Date endDateObject = Utils.getDate(cup.getProperty("timeShowEnd"),new Date(0));
                    JSONObject objectGifts1 = new JSONObject((String) cup.getProperty("gifts_1"));
                    JSONObject objectGifts2 = new JSONObject((String) cup.getProperty("gifts_2"));
                    JSONObject objectGifts3 = new JSONObject((String) cup.getProperty("gifts_3"));
                    if(objectGifts1.has("keyCup")) {
                        cupFamilyModel = new CupFamilyModelResponse();
                        cupFamilyModel.eventName = eventName;
                        cupFamilyModel.timeShowStart = startDateObject;
                        cupFamilyModel.timeShowEnd = endDateObject;
                        cupFamilyModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts1.get("cupTop")));
                        cupFamilyModel.keyCup = (String) objectGifts1.get("keyCup");
                        cupFamilyModel.cupUrl = (String) objectGifts1.get("cupUrl");
                        if(objectGifts1.has("frameUrl")) {
                            cupFamilyModel.frameUrl = (String) objectGifts1.get("frameUrl");
                        }
                        response.cupFamilyForEvents.add(cupFamilyModel);
                    }
                    if(objectGifts2.has("keyCup")) {
                        cupFamilyModel = new CupFamilyModelResponse();
                        cupFamilyModel.eventName = eventName;
                        cupFamilyModel.timeShowStart = startDateObject;
                        cupFamilyModel.timeShowEnd = endDateObject;
                        cupFamilyModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts2.get("cupTop")));
                        cupFamilyModel.keyCup = (String) objectGifts2.get("keyCup");
                        cupFamilyModel.cupUrl = (String) objectGifts2.get("cupUrl");
                        if(objectGifts2.has("frameUrl")) {
                            cupFamilyModel.frameUrl = (String) objectGifts2.get("frameUrl");
                        }
                        response.cupFamilyForEvents.add(cupFamilyModel);
                    }
                    if(objectGifts3.has("keyCup")) {
                        cupFamilyModel = new CupFamilyModelResponse();
                        cupFamilyModel.eventName = eventName;
                        cupFamilyModel.timeShowStart = startDateObject;
                        cupFamilyModel.timeShowEnd = endDateObject;
                        cupFamilyModel.cupTop = getCupTypeString(Long.parseLong((String) objectGifts3.get("cupTop")));
                        cupFamilyModel.keyCup = (String) objectGifts3.get("keyCup");
                        cupFamilyModel.cupUrl = (String) objectGifts3.get("cupUrl");
                        if(objectGifts3.has("frameUrl")) {
                            cupFamilyModel.frameUrl = (String) objectGifts3.get("frameUrl");
                        }
                        response.cupFamilyForEvents.add(cupFamilyModel);
                    }
                }
            }
        }
        return response;
    }

//	public static void main(String[] args) throws ServletException, IOException {
//        Utils.setEnv("GOOGLE_APPLICATION_CREDENTIALS", "/Volumes/DATA/ikara4m.json");
//        RemoteApiOptions options = new RemoteApiOptions().server("ikara4m.appspot.com", 443)
//                .remoteApiPath("/remote-api").useServiceAccountCredential("ikara4m@appspot.gserviceaccount.com", "/Users//tranhongminh//inmobi//notes//ikara4m-1d7122c21b97.p12");
//        RemoteApiInstaller installer = new RemoteApiInstaller();
//        try {
//            installer.install(options);
//            
//            // call phia tren
////            MockHttpServletRequest request = new MockHttpServletRequest();
////            request.setServerName("www.ikara.co");
////            CupRequest cupRequest = new CupRequest();
////            cupRequest.cupFor =  "BAITHUHOT,PHONGKARA,GIATOC";
////            String paramString = DigitalSignature.encryption(Utils.serialize(cupRequest));
////            request.setParameter("parameters", paramString);
////            UpdateCup updateCup = new UpdateCup();
////            updateCup.doPost(request, new MockHttpServletResponse());
////            CupResponse cuprp = getCups(cupRequest);
////            
////            getListCupForEvent();
//            
//            // insert ContentEvent
////            Key key = KeyFactory.createKey("ContentEvent", "Recording20102022vi");
////            Entity entity = new Entity("ContentEvent", "Recording20112022vi");
////            entity.setProperty("gifts_1", "{\"notificationUrl\":\"https://ikara-data.s3.us-west-002.backblazeb2.com/images/giftitems/systemnotification.png\",\"keyCup\":\"CUP TOP 1 BAI THU\",\"cupTop\":\"1\",\"cupUrl\":\"https://s3.us-west-002.backblazeb2.com/ikara-data/images/gifts/a3862fa0-f707-cfdf-892f-6b83b7fcce04.png\",\"notificationMessage\":\"Thông báo\\ntoàn hệ thống\",\"frameMessage\":\"Khung top 1\\nTrong 1 ngày\",\"language\":\"vi\",\"message\":\"Cup TOP 1\\n20000 iCoin\",\"frameUrl\":\"https://storage.googleapis.com/ikara-storage/ikara/frame/top_1_fan_icon.webp\",\"cupUrlForEvent\":\"—\",\"lastUpdate\":\"Fri Oct 21 11:16:19 ICT 2022\",\"keyCupForEvent\":\"—\",\"cupFor\":\"BAITHUHOT\"}");
////            entity.setProperty("gifts_2", "{\"keyCup\":\"CUP TOP 2 BAI THU\",\"cupTop\":\"2\",\"cupUrl\":\"https://s3.us-west-002.backblazeb2.com/ikara-data/images/gifts/3309b3cf-d74f-82af-c4e1-612677153773.png\",\"frameMessage\":\"Khung top 2 Trong 1 ngày\",\"language\":\"vi\",\"message\":\"Cup TOP 2 10000 iCoin\",\"frameUrl\":\"https://storage.googleapis.com/ikara-storage/ikara/frame/top_2_fan_icon.webp\",\"cupUrlForEvent\":\"—\",\"lastUpdate\":\"Fri Oct 21 11:30:34 ICT 2022\",\"keyCupForEvent\":\"—\",\"cupFor\":\"BAITHUHOT\"}");
////            entity.setProperty("gifts_3", "{\"keyCup\":\"CUP TOP 3 BAI THU\",\"cupTop\":\"3\",\"cupUrl\":\"https://s3.us-west-002.backblazeb2.com/ikara-data/images/gifts/bd634b09-ed52-1a71-8f89-136452f0816d.png\",\"frameMessage\":\"Khung top 3 Trong 1 ngày\",\"language\":\"vi\",\"message\":\"Cup TOP 3 5000 iCoin\",\"frameUrl\":\"https://storage.googleapis.com/ikara-storage/ikara/frame/top_3_fan_icon.webp\",\"cupUrlForEvent\":\"—\",\"lastUpdate\":\"Fri Oct 21 11:32:34 ICT 2022\",\"keyCupForEvent\":\"—\",\"cupFor\":\"BAITHUHOT\"}");
////            entity.setProperty("gifts_4_10", "—");
////            entity.setProperty("mainRule", "{\"title\":\"Quy tắc\",\"details\":\"1. Bảng xếp hạng được sắp xếp dựa theo điểm bài thu. Điểm bài thu là tổng điểm quà tặng + điểm lượt nghe, lượt thích, lượt bình luận hợp lệ.\\n\\n2. Điểm quà tặng bằng 10 lần giá trị của quà tặng. Ví dụ quà tặng có giá 1 iCoin tương đương với 10 điểm, quà tặng có giá 8 iCoin tương đương với 80 điểm.\\n\\n3. Điểm cho mỗi lượt nghe, thích, bình luận được tính tùy thuộc vào Level của người đó. Cụ thể:\\n• Level 1-9 được 50 điểm\\n• Level 10-19 được 100 điểm\\n• Level 20-29 được 200 điểm\\n• Level 30-39 được 300 điểm\\n(Mỗi người chỉ được tính một lượt nghe, thích, bình luận hợp lệ)\"}");
////            entity.setProperty("otherRule", "{\"title\":\"Lưu ý\",\"details\":\" Trong thời gian tham gia, nếu phát hiện người dùng nào sử dụng các thủ đoạn bất thường để đua top, chúng tôi sẽ lập tức hủy quyền dự thi, khóa tài khoản mà không cần thông báo trước!\\n Các hành vi vi phạm bao gồm:\\n    1. Giả mạo hoặc mượn tài khoản người khác để thi.\\n    2. Tạo nhiều tài khoản (cùng một chủ) để dự thi.\\n    3. Sử dụng hành vi gian lận điểm số.\\n    4. Sử dụng bình luận ác ý với bài hát của người khác.\\nQuyền giải thích cuối cùng thuộc về đội ngũ của iKara - Yokara.\"}");
////            entity.setProperty("timeActiveStart", "October 20, 2022 at 3:41:45 PM UTC+7");
////            entity.setProperty("timeActiveEnd", "October 20, 2022 at 3:41:45 PM UTC+7");
////            entity.setProperty("timeShowStart", "October 19, 2022 at 3:41:45 PM UTC+7");
////            entity.setProperty("timeShowEnd", "October 20, 2022 at 3:41:45 PM UTC+7");
////            entity.setProperty("timeRule", "{\"title\":\"Thời gian\",\"details\":\"Bắt đầu lúc 21:00 mỗi ngày, kết thúc 20:59 ngày hôm sau\"}");
////            entity.setProperty("eventName", "Ngày 20-11-2022");
////            datastore.put(entity);
//            
//            // update data
////            Key key = KeyFactory.createKey("ContentEvent", "Recording20112022vi");
////            Entity entity = datastore.get(key);
////            entity.setProperty("eventName", "Ngày 21-10-2022");
////            datastore.put(entity);
//            
//            
//            
//            
//            // update data CupRank
////            Key key = KeyFactory.createKey("CupRank", 4509053559898112L);
////            Entity entity = datastore.get(key);
//////            entity.setProperty("frameMessage", "Khung top 3 Trong 1 ngày");
//////            entity.setProperty("frameUrl", "https://storage.googleapis.com/ikara-storage/ikara/frame/top_3_fan_icon.webp");
////            entity.setProperty("message", "1 Bó hoa 25 iCoin");
//////            entity.setProperty("notificationMessage", "Thông báo toàn hệ thống");
//////            entity.setProperty("notificationUrl", "https://ikara-data.s3.us-west-002.backblazeb2.com/images/giftitems/systemnotification.png");
////            datastore.put(entity);
////            
////            Query query = new Query("CupRank");
//////          .addFilter("cupUrl", FilterOperator.EQUAL, request.imgUrl)
//////          .addFilter("cupTop", FilterOperator.EQUAL, request.cupTop);
////              PreparedQuery pq = datastore.prepare(query);
////              List<Entity> results = pq.asList(FetchOptions.Builder.withDefaults());
////              String cupUrlForEvent = "https://s3.us-west-002.backblazeb2.com/ikara-data/images/gifts/82b742fd-aa14-c15b-c304-7d72bb8fbb5b.png";
////              String keyCupForEvent = "CUP TOP 3 PHONG LIVE";
////              for (Entity cupRank : results) {
////                  String cupUrlForEventDb = (String) cupRank.getProperty("cupUrlForEvent");
////                  String keyCupForEventDb = (String) cupRank.getProperty("keyCupForEvent");
////                  if(cupUrlForEventDb != null && keyCupForEventDb != null
////                         && cupUrlForEventDb.equals(cupUrlForEvent) && keyCupForEventDb.equals(keyCupForEvent)) {
////                      cupRank.setProperty("cupUrlForEvent", "—");
////                      cupRank.setProperty("keyCupForEvent", "—");
////                      datastore.put(cupRank);
////                      break;
////                  }
////              }
////                  Entity entity2 = new Entity("CupRank");
////                entity2.setProperty("cupFor", LIVEROOM);
////                entity2.setProperty("cupTop", 3);
////                entity2.setProperty("cupUrl", cupUrlForEvent);
////                entity2.setProperty("keyCup", keyCupForEvent);
////                entity2.setProperty("language", "vi");
////                
////                datastore.put(entity2);
//              
////              for (Entity cupRank : results) {
////                  String cupFor = (String) cupRank.getProperty("cupFor");
////                  if("Bài thu hot".equals(cupFor)) {
////                      cupRank.setProperty("cupFor", RECORDINGHOT);
////                      datastore.put(cupRank);
////                  } else if("Phòng kara".equals(cupFor)) {
////                      cupRank.setProperty("cupFor", LIVEROOM);
////                      datastore.put(cupRank);
////                  } else if("Gia tộc".equals(cupFor)) {
////                      cupRank.setProperty("cupFor", FAMILY);
////                      datastore.put(cupRank);
////                  }
////              }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            installer.uninstall();
//        }
//       
//    }
    
   @POST
//  @Path("/getCups")
  @Consumes({ MediaType.APPLICATION_JSON })
  @Produces({ MediaType.APPLICATION_JSON })
    public static CupModelResponse getCupsWithDate(CupRequest request) {
        CupModelResponse response = new CupModelResponse();
        response= getListCupForEventWithDate();
        if(request.cupFor != null && request.cupFor.contains(",")) {
            // init
            String[] cupForArr = request.cupFor.split(",");
            for (String cupFor : cupForArr) {
                if(RECORDINGHOT.equals(cupFor.trim())) {
                    response.cupRecordingHotModels = new ArrayList<>();
//                    response.cupRecordingHotForEvents = new ArrayList<>();
                    Query query = new Query("CupRank").addFilter("cupFor", FilterOperator.EQUAL, RECORDINGHOT);
                    //.addSort("cupTop", SortDirection.ASCENDING);
                    PreparedQuery pq = datastore.prepare(query);
                    List<Entity> results = pq.asList(FetchOptions.Builder.withDefaults());
                    List<Long> listCupTop = new ArrayList<>();
                    for (Entity entity : results) {
                        listCupTop.add((long) entity.getProperty("cupTop"));
                    }
                    listCupTop.sort(Comparator.naturalOrder());
                    Set<Long> targetSet = new HashSet<>(listCupTop);
                    List<Long> targetList = new ArrayList<>(targetSet);
                    for (Long long1 : targetList) {
                        for (Entity entity : results) {
                            if(IDNORMAL.contains(Long.toString(entity.getKey().getId()))
                                    && long1 == (long) entity.getProperty("cupTop")) {
//                                CupRecordingHotModel cupRecordingHotModel = new CupRecordingHotModel();
//                                cupRecordingHotModel.cupTop = getCupTypeString((long) entity.getProperty("cupTop"));
//                                cupRecordingHotModel.cupUrl = (String) entity.getProperty("cupUrl");
//                                cupRecordingHotModel.keyCup = (String) entity.getProperty("keyCup");
//                                response.cupRecordingHotModels.add(cupRecordingHotModel);
                                CupRecordingHotModelResponse cupRecordingHotModelResponse = new CupRecordingHotModelResponse();
                                cupRecordingHotModelResponse.cupTop = getCupTypeString((long) entity.getProperty("cupTop"));
                                cupRecordingHotModelResponse.cupUrl = (String) entity.getProperty("cupUrl");
                                cupRecordingHotModelResponse.keyCup = (String) entity.getProperty("keyCup");
                                response.cupRecordingHotModels.add(cupRecordingHotModelResponse);
                            } else if(long1 == (long) entity.getProperty("cupTop")) {
//                                CupRecordingHotModel cupRecordingHotModel = new CupRecordingHotModel();
//                                cupRecordingHotModel.cupTop = getCupTypeString((long) entity.getProperty("cupTop"));
//                                cupRecordingHotModel.keyCupForEvent = (String) entity.getProperty("keyCup");
//                                cupRecordingHotModel.cupUrlForEvent = (String) entity.getProperty("cupUrl");
//                                response.cupRecordingHotForEvents.add(cupRecordingHotModel);
                                CupRecordingHotModelResponse cupRecordingHotModelResponse = new CupRecordingHotModelResponse();
                                cupRecordingHotModelResponse.cupTop = getCupTypeString((long) entity.getProperty("cupTop"));
                                cupRecordingHotModelResponse.cupUrl = (String) entity.getProperty("cupUrl");
                                cupRecordingHotModelResponse.keyCup = (String) entity.getProperty("keyCup");
                                response.cupRecordingHotModels.add(cupRecordingHotModelResponse);
                                
                            }
                        }
                        response.status = "OK";
                    }
                } else if(LIVEROOM.equals(cupFor.trim())) {
                    response.cupLiveRoomModels = new ArrayList<>();
//                    response.cupLiveRoomForEvents = new ArrayList<>();
                    Query query = new Query("CupRank").addFilter("cupFor", FilterOperator.EQUAL, LIVEROOM);
                    PreparedQuery pq = datastore.prepare(query);
                    List<Entity> results = pq.asList(FetchOptions.Builder.withDefaults());
                    List<Long> listCupTop = new ArrayList<>();
                    for (Entity entity : results) {
                        listCupTop.add((long) entity.getProperty("cupTop"));
                    }
                    listCupTop.sort(Comparator.naturalOrder());
                    Set<Long> targetSet = new HashSet<>(listCupTop);
                    List<Long> targetList = new ArrayList<>(targetSet);
                    for (Long cupTop : targetList) {
                        for (Entity entity : results) {
                            if(IDNORMAL.contains(Long.toString(entity.getKey().getId()))
                                    && cupTop == (long) entity.getProperty("cupTop") && "vi".equals(entity.getProperty("language"))) {
//                                CupLiveRoomModel cupLiveRoomModel = new CupLiveRoomModel();
//                                cupLiveRoomModel.cupTop = getCupTypeString((long) entity.getProperty("cupTop"));
//                                cupLiveRoomModel.cupUrl = (String) entity.getProperty("cupUrl");
//                                cupLiveRoomModel.keyCup = (String) entity.getProperty("keyCup");
//                                response.cupLiveRoomModels.add(cupLiveRoomModel);
                                CupRecordingHotModelResponse cupRecordingHotModelResponse = new CupRecordingHotModelResponse();
                                cupRecordingHotModelResponse.cupTop = getCupTypeString((long) entity.getProperty("cupTop"));
                                cupRecordingHotModelResponse.cupUrl = (String) entity.getProperty("cupUrl");
                                cupRecordingHotModelResponse.keyCup = (String) entity.getProperty("keyCup");
                                response.cupRecordingHotModels.add(cupRecordingHotModelResponse);
                            } else if(cupTop == (long) entity.getProperty("cupTop") && "vi".equals(entity.getProperty("language"))) {
//                                CupLiveRoomModel cupLiveRoomModel = new CupLiveRoomModel();
//                                cupLiveRoomModel.cupTop = getCupTypeString((long) entity.getProperty("cupTop"));
//                                cupLiveRoomModel.keyCupForEvent = (String) entity.getProperty("keyCup");
//                                cupLiveRoomModel.cupUrlForEvent = (String) entity.getProperty("cupUrl");
//                                response.cupLiveRoomForEvents.add(cupLiveRoomModel);
                                CupRecordingHotModelResponse cupRecordingHotModelResponse = new CupRecordingHotModelResponse();
                                cupRecordingHotModelResponse.cupTop = getCupTypeString((long) entity.getProperty("cupTop"));
                                cupRecordingHotModelResponse.cupUrl = (String) entity.getProperty("cupUrl");
                                cupRecordingHotModelResponse.keyCup = (String) entity.getProperty("keyCup");
                                response.cupRecordingHotModels.add(cupRecordingHotModelResponse);
                            }
                        }
                        response.status = "OK";
                    }
                } else if(FAMILY.equals(cupFor.trim())) {
                    response.cupFamilyModels = new ArrayList<>();
                    Query query = new Query("CupRank").addFilter("cupFor", FilterOperator.EQUAL, FAMILY);
                    PreparedQuery pq = datastore.prepare(query);
                    List<Entity> results = pq.asList(FetchOptions.Builder.withDefaults());
                    List<Long> listCupTop = new ArrayList<>();
                    for (Entity entity : results) {
                        listCupTop.add((long) entity.getProperty("cupTop"));
                    }
                    listCupTop.sort(Comparator.naturalOrder());
                    for (Long long1 : listCupTop) {
                        for (Entity entity : results) {
                            if(long1 == (long) entity.getProperty("cupTop")) {
//                                CupFamilyModel cupFamilyModel = new CupFamilyModel();
//                                cupFamilyModel.cupTop = getCupTypeString((long) entity.getProperty("cupTop"));
//                                cupFamilyModel.cupUrl = (String) entity.getProperty("cupUrl");
//                                cupFamilyModel.keyCup = (String) entity.getProperty("keyCup");
//                                response.cupFamilyModels.add(cupFamilyModel);
                                CupFamilyModelResponse cupRecordingHotModelResponse = new CupFamilyModelResponse();
                                cupRecordingHotModelResponse.cupTop = getCupTypeString((long) entity.getProperty("cupTop"));
                                cupRecordingHotModelResponse.cupUrl = (String) entity.getProperty("cupUrl");
                                cupRecordingHotModelResponse.keyCup = (String) entity.getProperty("keyCup");
                                response.cupFamilyModels.add(cupRecordingHotModelResponse);
                            }
                        }
                        response.status = "OK";
                    }
                }
            }
        } else if(request.keyCup != null && request.imgUrl != null) {
            // update cup
            if(request.keyCupNew != null && request.imgUrlNew != null) {
                Query query = new Query("CupRank").addFilter("keyCup", FilterOperator.EQUAL, request.keyCup);
//                        .addFilter("cupUrl", FilterOperator.EQUAL, request.imgUrl)
//                        .addFilter("cupTop", FilterOperator.EQUAL, request.cupTop);
                PreparedQuery pq = datastore.prepare(query);
                List<Entity> results = pq.asList(FetchOptions.Builder.withDefaults());
                long cupTopRq = getCupTypeInt(request.cupTop);
                for (Entity cupRank : results) {
                    long cupTopDb = (long) cupRank.getProperty("cupTop");
                    if(request.imgUrl.equals(cupRank.getProperty("cupUrl")) && cupTopDb == cupTopRq) {
                        cupRank.setProperty("keyCup", request.keyCupNew);
                        cupRank.setProperty("cupUrl", request.imgUrlNew);
                        datastore.put(cupRank);
                        response.status = "OK";
                        response.message = "Update thành công!";
                        break;
                    }
                }
//                Entity cupRank = pq.asSingleEntity();
            } else {
                response.status = "NG";
                response.message = "Update thất bại! (Bạn chưa chọn CUP mới)";
            }
        } else if(request.keyCupForEvent != null && request.cupUrlForEvent != null) {
            // Su Kien: update cup
            if(request.keyCupForEventNew != null && request.cupUrlForEventNew != null) {
                Query query = new Query("CupRank").addFilter("keyCup", FilterOperator.EQUAL, request.keyCupForEvent);
                PreparedQuery pq = datastore.prepare(query);
//                Entity cupRank = pq.asSingleEntity();
                List<Entity> results = pq.asList(FetchOptions.Builder.withDefaults());
                long cupTopRq = getCupTypeInt(request.cupTop);
                for (Entity cupRank : results) {
                    long cupTopDb = (long) cupRank.getProperty("cupTop");
                    if(request.cupUrlForEvent.equals(cupRank.getProperty("cupUrl"))
                            && request.keyCupForEvent.equals(cupRank.getProperty("keyCup"))
                            && request.cupFor.equals(cupRank.getProperty("cupFor"))
                            && cupTopDb == cupTopRq) {
                        if(request.displayCupStart != null && request.displayCupEnd != null
                                && request.playingStart != null && request.playingEnd != null) {
                            cupRank.setProperty("keyCup", request.keyCupForEventNew);
                            cupRank.setProperty("cupUrl", request.cupUrlForEventNew);

                            cupRank.setProperty("displayCupStart", request.displayCupStart);
                            cupRank.setProperty("displayCupEnd", request.displayCupEnd);
                            cupRank.setProperty("playingStart", request.playingStart);
                            cupRank.setProperty("playingEnd", request.playingEnd);
                            datastore.put(cupRank);
                            response.status = "OK";
                            response.message = "Set thành công!";
                            break;
                        } else {
                            response.status = "NG";
                            response.message = "Set thất bại! (Bạn chưa chọn ngày giờ)";
                        }
                    }
                }
            } else {
                response.status = "NG";
                response.message = "Set thất bại! (Bạn chưa chọn CUP mới)";
            }
        }
        return response;
    }
  
    
//    public static void main(String[] args) throws Exception {    
//        RemoteApiOptions options = new RemoteApiOptions().server("ikara-development.appspot.com", 443)
//                .remoteApiPath("/remote-api").useServiceAccountCredential("ikara-development@appspot.gserviceaccount.com",
//                        "D:\\PROJECT\\ikara-development-3b1d5505d713.p12");
//        RemoteApiInstaller installer = new RemoteApiInstaller();
//
//
//  try {
//      installer.install(options);
////  MockHttpServletRequest request = new MockHttpServletRequest();
////  request.setServerName("www.ikara.co");
////  CupRequest cupRequest = new CupRequest();
////  cupRequest.cupFor =  "BAITHUHOT,PHONGKARA,GIATOC";
////  String paramString = DigitalSignature.encryption(Utils.serialize(cupRequest));
////  request.setParameter("parameters", paramString);
//// CupModelResponse cupModelResponse = getCupsWithDate(cupRequest);
//////  CupModelResponse cupModelResponse = getCups(request);
//// 
//// System.out.println(cupModelResponse);
//
//
//  } catch (Exception e) {
//      e.printStackTrace();
//  } finally {
//      installer.uninstall();
//
//  }
//}
}
