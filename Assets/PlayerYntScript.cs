using System.Collections;
using UnityEngine;
using TMPro;
using UnityEngine.UI;
using UnityEngine.Video;
using System.IO;
using UnityEngine.Android;

public class PlayerYntScript : MonoBehaviour
{
    public TextMeshProUGUI infoText, yukleniyor;
    public GameObject yukleniyorArkaplan;
    public Button selectButton;
    public VideoPlayer videoPlayer;
    private bool isPlaying = false;
    private string currentFileName = "";
    private string currentFilePath = "";
    public RenderTexture rt;
    public GameObject kup;

    public Vector3 androidScale;
    public Vector3 windowScale;
    public Slider Videoilerigerislider;
    public Button fullscreenBtn;
    public Slider ses;
    bool isDragging = false;
    public Transform seshareket;
    public float maxDistance = 15f;
    public float minDistance = 1f;
    private Vector3 startPosition;
    public TextMeshProUGUI videosuresi;

    private AndroidJavaClass filePickerClass;
    private AudioSource audioSource;
    bool stop;
    public GameObject seskayit, sesbitir;

    public void kayitbitirigetir()
    {
        sesbitir.SetActive(true);
        seskayit.SetActive(false);
    }

    public void seskayitedenigetir()
    {
        sesbitir.SetActive(false);
        seskayit.SetActive(true);
    }


    //void kulaklıksesayarlari()
    //{

    //    using (AndroidJavaClass ajc = new AndroidJavaClass("com.unity3d.player.SesYonetici"))
    //    {
    //        ajc.CallStatic("SesModunuGuncelle");
    //    }

    //    // 2. Mikrofonu tek bir kez başlat
    //    // null: varsayılan cihaz, true: döngüsel, 1 saniye buffer, 44100 frekans
    //    AudioClip mikrofonClip = Microphone.Start(null, true, 1, 44100);

    //    // 3. AudioSource bileşenini al veya ekle
    //    AudioSource sesCikis = GetComponent<AudioSource>();
    //    if (sesCikis == null) sesCikis = gameObject.AddComponent<AudioSource>();

    //    // 4. Mikrofon verisini hoparlöre (AudioSource) bağla
    //    sesCikis.clip = mikrofonClip;
    //    sesCikis.loop = true;

    //    // Mikrofonun açılmasını bekle (Android cihazlarda anlık değil, 0.1 saniye gerekebilir)
    //    while (!(Microphone.GetPosition(null) > 0)) { }
    //    sesCikis.Play();


    //}



    void Start()
    {
        Application.runInBackground = true;
        Application.targetFrameRate = 60;
        audioSource = GetComponent<AudioSource>();
       

        if (yukleniyor != null) yukleniyor.text = "";
        if (yukleniyorArkaplan != null) yukleniyorArkaplan.SetActive(false);

#if !UNITY_EDITOR && UNITY_ANDROID
        if (!Permission.HasUserAuthorizedPermission(Permission.ExternalStorageRead))
        {
            Permission.RequestUserPermission(Permission.ExternalStorageRead);
        }
        if (!Permission.HasUserAuthorizedPermission(Permission.Microphone))
        {
            Permission.RequestUserPermission(Permission.Microphone);
        }
#endif

#if UNITY_ANDROID && !UNITY_EDITOR
        filePickerClass = new AndroidJavaClass("com.unity3d.player.FilePicker");
#endif

        videosuresi.text = "00:00 / 00:00 ";

        if (Application.platform == RuntimePlatform.WindowsPlayer)
        {
            kup.transform.localScale = new Vector3(2853.828f, 1499.375f, 1f);
        }

        infoText.text = "Dosya Sec";
        videoPlayer.prepareCompleted += OnVideoPrepared;
        videoPlayer.started += OnVideoStarted;
        videoPlayer.Prepare();
        videoPlayer.audioOutputMode = VideoAudioOutputMode.Direct;

        startPosition = seshareket.transform.position;
        if (ses != null)
        {
            ses.minValue = 0f;
            ses.maxValue = 1f;
            ses.value = 1f;
            ses.onValueChanged.AddListener(UpdatePositionAndVolume);
        }
        UpdatePositionAndVolume(ses.value);
    }

    void Update()
    {
        if (isDragging) return;

        if (videoPlayer != null && videoPlayer.isPlaying)
        {
            if (videoPlayer.length > 0)
            {
                Videoilerigerislider.value = (float)videoPlayer.time;
                guncelmedyasuresi((float)videoPlayer.time, (float)videoPlayer.length);
            }
            else
            {
                Videoilerigerislider.value = 0f;
                videosuresi.text = "Canli Yayin";
            }
        }

        if (Input.GetKeyDown(KeyCode.Escape))
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
                AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
                AndroidJavaObject intent = new AndroidJavaObject("android.content.Intent", "android.intent.action.MAIN");
                intent.Call<AndroidJavaObject>("addCategory", "android.intent.category.HOME");
                intent.Call<AndroidJavaObject>("setFlags", 0x10000000);
                currentActivity.Call("startActivity", intent);
            }
            catch (System.Exception e) { Debug.LogError("Geri tusu hatasi: " + e.Message); }
#endif
        }
    }

    void guncelmedyasuresi(float currentTime, float totalTime)
    {
        if (videosuresi == null) return;
        if (totalTime <= 0)
        {
            videosuresi.text = "00:00 / 00:00";
            return;
        }

        int curMin = (int)(currentTime / 60f);
        int curSec = (int)(currentTime % 60f);
        int totMin = (int)(totalTime / 60f);
        int totSec = (int)(totalTime % 60f);
        videosuresi.text = string.Format("{0:00}:{1:00} / {2:00}:{3:00}", curMin, curSec, totMin, totSec);
    }

    void OnVideoPrepared(VideoPlayer vp)
    {
        Videoilerigerislider.maxValue = (float)vp.length;
        Videoilerigerislider.value = 0f;
    }

    void OnVideoStarted(VideoPlayer vp)
    {
        isPlaying = true;
        infoText.text = "Dosya Sec: " + currentFileName;

        if (yukleniyor != null) yukleniyor.text = "";
        if (yukleniyorArkaplan != null) yukleniyorArkaplan.SetActive(false);
    }

    public void OnSliderValueChanged(float value)
    {
        if (!isDragging) return;

        if (videoPlayer != null && videoPlayer.isPrepared)
        {
            videoPlayer.time = value;
        }
    }

    public void OnBeginDrag()
    {
        isDragging = true;
    }

    public void OnEndDrag()
    {
        isDragging = false;

        if (videoPlayer != null)
        {
            videoPlayer.time = Videoilerigerislider.value;
            videoPlayer.Play();
        }
    }

    void UpdatePositionAndVolume(float sliderValue)
    {
        float distance = Mathf.Lerp(minDistance, maxDistance, 1f - sliderValue);
        seshareket.transform.position = startPosition + Vector3.back * distance;
        float volume = Mathf.InverseLerp(maxDistance, minDistance, distance);

        if (videoPlayer != null) videoPlayer.SetDirectAudioVolume(0, volume);
        if (audioSource != null) audioSource.volume = volume;

#if UNITY_ANDROID && !UNITY_EDITOR
        try
        {
            AndroidJavaClass muzikServisi = new AndroidJavaClass("com.unity3d.player.FilePicker$MuzikServisi");
            muzikServisi.CallStatic("setJavaVolume", volume);
        }
        catch (System.Exception e) { Debug.LogError("Java ses ayari iletilemedi: " + e.Message); }
#endif
    }

    public void OnButtonClick()
    {
        if (yukleniyor != null) yukleniyor.text = "";
        if (yukleniyorArkaplan != null) yukleniyorArkaplan.SetActive(false);

        if (isPlaying)
        {
            StopMedia();
        }
        else
        {
            OpenFilePicker();
        }
    }

    void OpenFilePicker()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (filePickerClass != null)
        {
            filePickerClass.CallStatic("pickVideo", gameObject.name);
        }
        else
        {
            Debug.LogError("FilePicker sinifi bulunamadi!");
            infoText.text = "Eklenti Hatasi";
        }
#else
        Debug.Log("Unity Editordesiniz. Dosya secici sadece Android telefonda calisacak.");
#endif
    }

    public void PlayDirectUrl(string urlPath, string fileName)
    {
        if (string.IsNullOrEmpty(urlPath)) return;

        fileName = fileName.Replace("%20", " ");
        string ext = Path.GetExtension(fileName).ToLower();

        if (ext == ".wma" || urlPath.ToLower().Contains(".wma") || fileName.ToLower().Contains(".wma"))
        {
            infoText.text = "Dosya Sec";
            if (yukleniyor != null) yukleniyor.text = "WMA Desteklenmiyor! Yonlendiriliyorsunuz...";
            if (yukleniyorArkaplan != null) yukleniyorArkaplan.SetActive(true);
            Application.OpenURL("https://www.google.com/search?q=wma+mp3+d%C3%B6n%C3%BC%C5%9Ft%C3%BCr%C3%BCc%C3%BC");
            return;
        }

        StopForegroundService();

        if (videoPlayer != null && videoPlayer.isPlaying) videoPlayer.Stop();
        if (audioSource != null && audioSource.isPlaying) audioSource.Stop();

        currentFileName = fileName;
        currentFilePath = urlPath;

        bool gercektenVideoMu = true;

        if (ext == ".mp3" || ext == ".m4a" || ext == ".wav" || ext == ".ogg")
        {
            gercektenVideoMu = false;
        }
        else if (urlPath.StartsWith("content://"))
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                AndroidJavaClass filePicker = new AndroidJavaClass("com.unity3d.player.FilePicker");
                gercektenVideoMu = filePicker.CallStatic<bool>("dosjadaGoruntuVarMi", urlPath);
            }
            catch (System.Exception e)
            {
                Debug.LogError("Goruntu tarama hatasi: " + e.Message);
                gercektenVideoMu = false; 
            }
#else
            gercektenVideoMu = false;
#endif
        }

        if (!gercektenVideoMu)
        {
            if (yukleniyor != null) yukleniyor.text = "";
            if (yukleniyorArkaplan != null) yukleniyorArkaplan.SetActive(false);

            infoText.text = "Dosya Sec: " + currentFileName;
            isPlaying = true;
            StartForegroundService(urlPath);
            UpdatePositionAndVolume(ses.value);
        }
        else
        {
            if (yukleniyor != null) yukleniyor.text = "";
            if (yukleniyorArkaplan != null) yukleniyorArkaplan.SetActive(false);

            if (videoPlayer != null)
            {
                infoText.text = "Dosya Sec: " + currentFileName;
                videoPlayer.source = UnityEngine.Video.VideoSource.Url;
                videoPlayer.url = urlPath;
                videoPlayer.skipOnDrop = true;
                videoPlayer.Prepare();
                videoPlayer.Play();
            }
        }
    }

    void StartForegroundService(string urlPath)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        try
        {
            AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            AndroidJavaObject context = currentActivity.Call<AndroidJavaObject>("getApplicationContext");

            AndroidJavaObject intent = new AndroidJavaObject("android.content.Intent");
            intent.Call<AndroidJavaObject>("setClassName", context, "com.unity3d.player.FilePicker$MuzikServisi");
            intent.Call<AndroidJavaObject>("putExtra", "urlPath", urlPath);
            
            currentActivity.Call<AndroidJavaObject>("startForegroundService", intent);
        }
        catch (System.Exception e) { Debug.LogError("Servis hatasi: " + e.Message); }
#endif
    }

    void StopForegroundService()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        try
        {
            AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            AndroidJavaObject context = currentActivity.Call<AndroidJavaObject>("getApplicationContext");

            AndroidJavaObject intent = new AndroidJavaObject("android.content.Intent");
            intent.Call<AndroidJavaObject>("setClassName", context, "com.unity3d.player.FilePicker$MuzikServisi");

            currentActivity.Call<bool>("stopService", intent);
        }
        catch (System.Exception e) { Debug.LogError("Servis durdurma hatasi: " + e.Message); }
#endif
    }

    public void StopMedia()
    {
        if (videoPlayer != null) videoPlayer.Stop();
        if (audioSource != null) audioSource.Stop();
        isPlaying = false;

        infoText.text = "Dosya Sec";
        if (yukleniyor != null) yukleniyor.text = "";
        if (yukleniyorArkaplan != null) yukleniyorArkaplan.SetActive(false);
        StopForegroundService();
    }

   public void ClearText()
    {
        yukleniyor.text = "";
        yukleniyorArkaplan.SetActive(false);
    }

    public void ToggleFullscreen()
    {
        Screen.fullScreen = !Screen.fullScreen;
        if (Screen.fullScreen)
        {
            Screen.orientation = ScreenOrientation.Portrait;
            kup.transform.localScale = new Vector3(1250f, 1250f, 1f);

            yukleniyor.text = "Ekran Dönmesi Kapatıldı";
            
            yukleniyorArkaplan.SetActive(true);
          
            Invoke("ClearText", 3.0f);

          
           
        }
        else
        {
            Screen.orientation = ScreenOrientation.AutoRotation;
        }
    }

    public void OnVideoPicked(string videoPath)
    {
        if (string.IsNullOrEmpty(videoPath)) return;

        if (videoPath == "KULLANICI_IPTAL")
        {
            infoText.text = "Dosya Sec";
            if (yukleniyor != null) yukleniyor.text = "Seçim iptal edildi.";
            if (yukleniyorArkaplan != null) yukleniyorArkaplan.SetActive(true);
            return;
        }

        if (videoPath.StartsWith("HATA") || videoPath.StartsWith("JAVA_HATASI"))
        {
            infoText.text = "Dosya Sec";
            if (yukleniyor != null) yukleniyor.text = "Hata oluştu:\n" + videoPath;
            if (yukleniyorArkaplan != null) yukleniyorArkaplan.SetActive(true);
            return;
        }

        if (videoPath.StartsWith("YUKLENIYOR:"))
        {
            infoText.text = "Dosya Sec";
            if (yukleniyor != null)
            {
                yukleniyor.text = videoPath.Replace("YUKLENIYOR:", "Yükleniyor: ");
            }
            if (yukleniyorArkaplan != null) yukleniyorArkaplan.SetActive(true);
            return;
        }

        string temizlenmisIsim = Path.GetFileName(videoPath).Replace("%20", " ");
        PlayDirectUrl(videoPath, temizlenmisIsim);
    }

    public void SesiKaydetmeyeBasla()
    {
        if (!Permission.HasUserAuthorizedPermission(Permission.Microphone))
        {
            Permission.RequestUserPermission(Permission.Microphone);
            return;
        }

        try
        {
            // SesYonetici sınıfını çağıran bloğu sildik.
            // Unity Microphone.Start çağrısını sildik.

#if UNITY_ANDROID && !UNITY_EDITOR
        if (filePickerClass != null)
        {
            filePickerClass.CallStatic("startVoiceRecording");
        }
#endif
        }
        catch (System.Exception e)
        {
            Debug.LogError("Kayit baslatma hatasi: " + e.Message);
        }
    }

    public void SesiKaydetmeyiDurdur()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        try
        {
            if (filePickerClass != null)
            {
                filePickerClass.CallStatic("stopVoiceRecording");
            }
        }
        catch (System.Exception e)
        {
            Debug.LogError("Kayit durdurma hatasi: " + e.Message);
        }
#endif
    }

    public void OnVoiceRecordStatus(string status)
    {
        if (status == "KAYIT_BASLADI")
        {
            if (yukleniyor != null) yukleniyor.text = "Stüdyo kaydı başladı... Sesiniz kaydediliyor.";
            if (yukleniyorArkaplan != null) yukleniyorArkaplan.SetActive(true);
        }
        else if (status.StartsWith("KAYIT_BITTI:"))
        {
            string rawPath = status.Replace("KAYIT_BITTI:", "");
            string temizYol = rawPath.Replace("ok", "");

            if (yukleniyor != null)
            {
                yukleniyor.text = "Kayıt tamamlandı!\nDosya yolu: " + temizYol;
            }
            if (yukleniyorArkaplan != null) yukleniyorArkaplan.SetActive(true);
        }
        else if (status.StartsWith("HATA"))
        {
            if (yukleniyor != null) yukleniyor.text = status;
        }
    }

    private void YuklenmePaneliniKapat()
    {
        if (yukleniyorArkaplan != null) yukleniyorArkaplan.SetActive(false);
    }
}