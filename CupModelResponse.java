package vnapps.ikara.servlet.webModel;

import java.util.ArrayList;
import java.util.Date;

public class CupModelResponse {
    public String message;
    public String status;
    
    public ArrayList<CupRecordingHotModelResponse> cupRecordingHotModels;
    public ArrayList<CupLiveRoomModelResponse> cupLiveRoomModels;
    public ArrayList<CupFamilyModelResponse> cupFamilyModels;

    public ArrayList<CupRecordingHotModelResponse> cupRecordingHotForEvents;
    public ArrayList<CupLiveRoomModelResponse> cupLiveRoomForEvents;
    public ArrayList<CupFamilyModelResponse> cupFamilyForEvents;
    
    public ArrayList<EventNameResponse> eventRecordingHotNames;
    public ArrayList<EventNameResponse> eventLiveRoomNames;
    public ArrayList<EventNameResponse> eventFamilyNames;
}
