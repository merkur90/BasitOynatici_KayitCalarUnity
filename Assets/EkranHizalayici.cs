using System.Collections;
using System.Collections.Generic;
using UnityEngine;



public class EkranHizalayici : MonoBehaviour
{
    // Sahnedeki video oynayan küp objen
    public GameObject kup;
    //public GameObject ekrnzrlbtn;
    // Telefon DİKEYKEN küpün olması gereken boyut ve pozisyon
    private Vector3 dikeyKupBoyut = new Vector3(1250f, 1250f, 1f); // Burayı kendi göz kararına göre ayarla
    private Vector3 dikeyKupPozisyon = new Vector3(29.5f, -36f, -99f);

    // Telefon YATAYKEN küpün olması gereken boyut ve pozisyon (Tam ekran sığacak devasa boyutun)
    private Vector3 yatayKupBoyut = new Vector3(3570f, 1500f, 1f);
    private Vector3 yatayKupPozisyon = new Vector3(25f, -153f, -99f);

    // Takip mekanizması için son durum hafızası
    private bool sonYatayMi = false;

    void Start()
    {
        kup.transform.localScale = dikeyKupBoyut;

          // İlk açılışta ekran ne durumdaysa ona göre objeleri yerleştir
          sonYatayMi = Screen.width > Screen.height;
        ObjeleriHizala(sonYatayMi);
    }

    void Update()
    {
        // Anlık olarak ekranın yatay mı dikey mi olduğunu kontrol et
        bool suAnYatayMi = Screen.width > Screen.height;

        // Eğer kullanıcı telefonu çevirdiyse ve durum değiştiyse
        if (suAnYatayMi != sonYatayMi)
        {
            sonYatayMi = suAnYatayMi;
            ObjeleriHizala(suAnYatayMi);
        }
    }

    // Objelerin şeklini ve konumunu değiştiren fonksiyon
    void ObjeleriHizala(bool yatayMod)
    {
        if (yatayMod)
        {
            // TELEFON YATAY OLUNCA:
            // Küpü senin o devasa boyutuna getiriyoruz
            kup.transform.localScale = yatayKupBoyut;
            kup.transform.localPosition = yatayKupPozisyon;

            // Varsa sahnedeki diğer objelerin (butonlar, yazılar vb.) yerini de buradan değiştirebilirsin
            // Örnek: buton.SetActive(false); // Yatayda butonu gizle gibi...
        }
        else
        {
            // TELEFON DİKEY OLUNCA:
            // Küpü dikey ekrana sığacak, üstte boşluk bırakacak makul boyutuna çekiyoruz
            kup.transform.localScale = dikeyKupBoyut;
            kup.transform.localPosition = dikeyKupPozisyon;

            // Örnek: buton.SetActive(true); // Dikeyde butonu geri göster...
        }
    }
}