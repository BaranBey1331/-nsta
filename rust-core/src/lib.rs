use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use reqwest::blocking::Client;
use scraper::{Html, Selector};
use regex::Regex;

#[no_mangle]
pub extern "system" fn Java_com_example_insta_MainActivity_resolveUrl(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    let url: String = env.get_string(&input).expect("Couldn't get java string!").into();
    
    let client = Client::builder()
        .user_agent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        .build()
        .unwrap();

    let response = client.get(&url).send();
    
    let video_url = match response {
        Ok(res) => {
            let body = res.text().unwrap_or_default();
            // Try meta tag first
            let document = Html::parse_document(&body);
            let selector = Selector::parse("meta[property='og:video']").unwrap();
            
            if let Some(element) = document.select(&selector).next() {
                element.value().attr("content").unwrap_or("").to_string()
            } else {
                // Fallback to regex for __additionalData
                let re = Regex::new(r#""video_url":"([^"]+)""#).unwrap();
                if let Some(caps) = re.captures(&body) {
                    caps.get(1).map_or("", |m| m.as_str()).replace("\\u0026", "&")
                } else {
                    "".to_string()
                }
            }
        }
        Err(_) => "".to_string(),
    };

    let output = env.new_string(video_url).expect("Couldn't create java string!");
    output.into_raw()
}
