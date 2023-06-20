
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class RunFFmpeg2 {

    public static final String AUDIO_ONLY = "140,43,251";
	public static final String VIDEO_AND_AUDIO = "17,18,22,36,37,38,59,78";
	public static final String VIDEO_ONLY = "133,134,135,136,137,138,160,264,266,298,299";
	
	public static final String AUDIO_OUTSIDE_YT = "mp3, webm, m4a";
	public static final String VIDEO_OUTSIDE_YT = "mp4, flv, 3gpp";
	
	private static final String SERVICE_ENDPOINT = "https://data4.ikara.co:9000";
	private static final String REGION = "us-east-1";
	private static final String ACCESS_KEY = "writeonlyuser";
	private static final String SECRET_KEY = "awJBPnZwvVpGYmp5MDQsw3Ry7z5EsyAE";
	private static final String BUCKET_NAME = "ikara-data";
    
    public static void main(String[] args) {
        File folder = new File("D:\\ffmpegFolder");
        File[] files = folder.listFiles();
        for (File file : files) {
            if(file.getPath().contains(".mp4")) {
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-v", "error", "-i", file.getPath(), "-f", "null", "-");
                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();
//                    Process process = Runtime.getRuntime().exec("ffmpeg -v error -i "+file.getPath() +" -f null - 2>D:\\ffmpegFolder\\error.log");
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    
                    if(bufferedReader.readLine() == null) {
//                       try {
//                           MessageDigest mDigest = MessageDigest.getInstance("MD5");
//                           FileInputStream inputFile = new FileInputStream(file.getPath());
//                           byte[] dataBytes = new byte[1024];
//                           int bytesRead;
//                           while ((bytesRead = inputFile.read(dataBytes)) != -1) {
//                               mDigest.update(dataBytes, 0, bytesRead);
//                           }
//                           byte[] mdBytes = mDigest.digest();
//                           
//                           StringBuilder stringBuilder = new StringBuilder();
//                           for (byte mdByte : mdBytes) {
//                               stringBuilder.append(Integer.toString((mdByte & 0xff) + 0x100, 16).substring(1));
//                           }
//                           
//                           System.out.println("MD5 hash of " + file.getPath() + " is: " + stringBuilder.toString());
//                       } catch (Exception e) {
//                        // TODO: handle exception
//                       }
                    }else {
                        String link = getDirectLinkYoutube(file.getName());
                        downloadMp4AndMp3FromYtLink(file.getName(),link);
                    }
                    
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    public static void downloadMp4AndMp3FromYtLink(String videoId, String linkYt) throws UnsupportedEncodingException {
		Thread thread = new Thread() {
			public void run(){
				try {
					HashMap<String, String> urlTags = getQueryMap(linkYt, "UTF-8");
					String strExtCode = null;
					String ext = null;
					
					if (urlTags.get("itag") != null) {
						strExtCode = urlTags.get("itag");
						int extCode = Integer.valueOf(strExtCode);
						ext = extCodeMap.get(extCode).toLowerCase();
					} else {
						String linkNoParams = linkYt.split("\\?")[0];
						int index = linkNoParams.lastIndexOf(".");
						ext = linkNoParams.substring(index + 1).toLowerCase();
					}	
					
					System.out.println("Downloading.... ");
					
					String url = "";
					if (ServerInfo.nameServerInt == 11 || ServerInfo.nameServerInt == 21) {
						url = Constants.KARAOKE_SONG_PATH + Constants.slashUse  + videoId;
					} else {
						url = Constants.DOWLOAD_YOUTUBE_PATH + Constants.slashUse  + videoId;	
					}
					System.out.println("save to url: " + url + "." + ext);
			
					try (BufferedInputStream in = new BufferedInputStream(new URL(linkYt).openStream());
						FileOutputStream fileOutputStream = new FileOutputStream(url + "." + ext)) {
						byte dataBuffer[] = new byte[4096];
						int bytesRead;
						while ((bytesRead = in.read(dataBuffer)) != -1) {
							fileOutputStream.write(dataBuffer, 0, bytesRead);
						}
						System.out.println("Download Finished!");								
					} catch (IOException e) {
						e.printStackTrace();
						System.err.println("File hoặc folder không tồn tại");
						return;
					}
					
					// // convert file
					// boolean isVideo = false;
					// // convert file outside YouTube
					// if (strExtCode == null) {
					// 	// convert video to mp4
					// 	if (VIDEO_OUTSIDE_YT.contains(ext)) {
					// 		isVideo = true;
					// 		convertVideoAndAudio(url, ext, "mp4");
					// 		convertVideoAndAudio(url, ext, "mp3");
					// 	}
					// 	// convert to mp3
					// 	if (AUDIO_OUTSIDE_YT.contains(ext)) {
					// 		convertVideoAndAudio(url, ext, "mp3");
					// 	}
					// } 
					// // convert file on YouTube
					// else {
					// 	// convert video to mp4
					// 	if (VIDEO_AND_AUDIO.contains(strExtCode)) {
					// 		isVideo = true;
					// 		convertVideoAndAudio(url, ext, "mp4");
					// 		convertVideoAndAudio(url, ext, "mp3");
					// 	}
					// 	// convert to mp3
					// 	if (AUDIO_ONLY.contains(strExtCode)) {
					// 		convertVideoAndAudio(url, ext, "mp3");
					// 	}
					// }
					// if (isVideo) {
					// 	uploadToServer(url, "mp4");
					// 	updateLinkMp4(videoId, ServerInfo.nameServerInt);
					// }
					// uploadToServer(url, "mp3");
					// updateLinkMp3(videoId, ServerInfo.nameServerInt);
					System.out.println("DONE");
				} catch (FileNotFoundException e) {
					System.err.println("Không tìm thấy file");
					e.printStackTrace();
				} catch (Exception e) {
					System.err.println("Lỗi không xác định");
					e.printStackTrace();
				}
			}
		};
		thread.start();
	}

    public static String getDirectLinkYoutube(String videoId){
        String videoAndAudio = "17,18,22,36,37,38,59,78";
        Date expiredDate = new Date(Long.MAX_VALUE);
        String stringResponse = "";
        HashMap<Integer, String> streamLinks = new HashMap<Integer, String>();
        try {
            String urlGetLInk = "https://www.youtube.com/watch?v=" + videoId;
          URL urls = new URL(urlGetLInk);
          BufferedReader in = new BufferedReader(new InputStreamReader(urls.openStream()));

          String inputLine;
          String outputLine = "";
          while ((inputLine = in.readLine()) != null) {
              outputLine = outputLine + inputLine;
        }           
          String contentString = getBase64ContentYtForSave(outputLine);
          ArrayList<String> arrayList = new ArrayList<>();
          arrayList.add(contentString);
          
          GetYtDirectLinksRequest getYtDirectLinksRequest = new GetYtDirectLinksRequest();
          getYtDirectLinksRequest.videoId = videoId;
          getYtDirectLinksRequest.contents = arrayList;
          
          String param = Utils.serialize(getYtDirectLinksRequest);
          HashMap<String, String> params = new HashMap<String, String>();
          params.put("parameters", Utils.serialize(getYtDirectLinksRequest));
          String links = null;
          links = Utils.sendPostRequest(
                  "http://data3.ikara.co:8080/ikaraweb/cgi-bin/GetYtDirectLinks.py", params);
          
          if (links != null && !links.trim().equals("")) {
              JSONObject object = new JSONObject(links);
              JSONArray linkArray = object.getJSONArray("links");

              for (int i = 0; i < linkArray.length(); i++) {
                  String link = linkArray.getString(i);
                  if (!link.trim().equals("")) {
                      HashMap<String, String> url = getQueryMap(link, "UTF-8");
                      if (url.get("itag") != null && videoAndAudio.contains(url.get("itag"))) {
                          streamLinks.put(Integer.parseInt(url.get("itag")), link);
                      }
                  }
              }
          }

          ArrayList<String> response = new ArrayList<>();
          
          for (Integer itag : streamLinks.keySet()) {
              if (Meta.typeMap.containsKey(itag)) {
                  Video cVideo = new Video(Meta.typeMap.get(itag).ext, Meta.typeMap.get(itag).type,
                          Meta.typeMap.get(itag).def, streamLinks.get(itag));
                  response.add(cVideo);
              }
          }
          
          Collections.sort(response, new Comparator<Video>() {
              @Override
              public int compare(Video o1, Video o2) {
                  return o1.quality.compareTo(o2.quality);
              }
          });

          for (int i = 0; i < response.size(); i++) {
              Video video = response.get(i);
              HashMap<String, String> url = getQueryMap(video.url, "UTF-8");
              if (url.get("expire") != null) {
                  Date cExpiredDate = new Date(Long.parseLong(url.get("expire")) * 1000);
                  if (cExpiredDate.before(new Date())) {

                      response.remove(i);
                      i--;
                  }
              }
          }
          boolean isContentNew = true;
          if (response.size() > 0) {

              for (Video video : response) {
                  HashMap<String, String> url = getQueryMap(video.url, "UTF-8");
                  if (url.get("expire") != null) {
                      Date cExpiredDate = new Date(Long.parseLong(url.get("expire")) * 1000);
                      if (cExpiredDate.before(expiredDate))
                          expiredDate = cExpiredDate;
                  }
              }
              if (isContentNew) {
              } else {
                  if (!Utils.exists(response.get(0).url)) {
                      response.clear();
                  }
              }
          }

          if(response.size() > 0){
            stringResponse = response.get(0).url;
          }
          
          in.close();
        } catch (Exception e) {
          System.out.println(e.getMessage());
        }
        
        return stringResponse;
    }

    public void convertVideoAndAudio(String url, String originExt, String targetExt) throws IOException, InterruptedException {
		if (originExt.toLowerCase().equals(targetExt))
			return;
		String command = Constants.FFMPEG_PATH + "  -hide_banner -loglevel panic -y -i " + url + "." + originExt + " -b:a 128K -vn " + url + "."  + targetExt;
//		System.out.println("YOUTUBE-DL: " + command);
		Utils.executeCommandLine(MainMixRecording.rt, command, 0);
	}

    // send post get link download youtube video
    public static String sendPostRequest(String urlInString, HashMap<String, String> parameters) throws Exception {
        String content = null;

        URL url = new URL(urlInString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");

        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.43 Safari/537.31");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        List<NameValuePair> params = new ArrayList<NameValuePair>();

        // String parameters =
        // vnapps.ikara.common.v13.Utils.serialize(sendEmailRequest);

        for (String key : parameters.keySet()) {
            params.add(new BasicNameValuePair(key, parameters.get(key)));
        }
        OutputStream os = connection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        String query = vnapps.ikara.common.v13.Utils.getQuery(params);

        // log.log(Level.WARNING, "SEND EMAIL TO SERVER 3 " + query);

        writer.write(query);
        writer.flush();
        writer.close();

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        StringBuffer res = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            res.append(line);
        }
        reader.close();

        content = res.toString();
        return content;
    }

    public static class GetYtDirectLinksRequest implements Serializable {
        public String videoId;
        public ArrayList<String> contents;
    }

    public static class Video  implements Serializable{
        public String type = ""; // Low Quality, Medium Quality, High Quality, Full High Quality, Original Definition
        public String url = "";
        public Integer quality;
        public String ext = "";
    
        public Video(String ext, String type, String def, String url) {
            this.type = type;
            this.ext = ext;
            this.url = url;
            this.quality = Integer.parseInt(def.substring(0, def.length() - 1));
        }
        
        public Video() {
        }
    }
}
