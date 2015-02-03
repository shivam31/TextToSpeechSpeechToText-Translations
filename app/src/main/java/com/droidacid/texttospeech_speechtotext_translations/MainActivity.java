package com.droidacid.texttospeech_speechtotext_translations;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Locale;


public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    EditText etTranslate;
    TextView tvTranslation;

    private Locale currentSpokenLang = Locale.US;
    private Locale locSpanish = new Locale("es", "MX");
    private Locale locRussian = new Locale("ru", "RU");
    private Locale locPortuguese = new Locale("pt", "BR");
    private Locale locDutch = new Locale("nl", "NL");
    private Locale[] languages = {locDutch, Locale.FRENCH, Locale.GERMAN,
            Locale.ITALIAN, locPortuguese, locRussian, locSpanish};

    private TextToSpeech textToSpeech;

    private Spinner languageSpinner;

    private int spinnerIndex = 0;

    private String[] arrayOfTranslations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        languageSpinner = (Spinner) findViewById(R.id.lang_spinner);

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int index, long id) {
                currentSpokenLang = languages[index];

                spinnerIndex = index;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        textToSpeech = new TextToSpeech(this, this);

        onTranslate(etTranslate);
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            super.onDestroy();
        }
    }


    protected boolean isEmpty(EditText translateText) {
        return translateText.getText().toString().trim().length() == 0;
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(currentSpokenLang);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language Not Supported", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "TTS Failed", Toast.LENGTH_LONG).show();
        }

    }

    public void onTranslate(View view) {

        etTranslate = (EditText) findViewById(R.id.words_edit_text);
        tvTranslation = (TextView) findViewById(R.id.translate_text_view);
        if (!isEmpty(etTranslate)) {
            Toast.makeText(this, "Getting translations",
                    Toast.LENGTH_LONG).show();

            new GetXMLData().execute();
        } else {
            Toast.makeText(this, "Enter words to translate",
                    Toast.LENGTH_SHORT).show();
        }

    }

    public void readTheText(View view) {

        textToSpeech.setLanguage(currentSpokenLang);

        if (arrayOfTranslations.length >= 9) {
            textToSpeech.speak(arrayOfTranslations[spinnerIndex + 4], TextToSpeech.QUEUE_FLUSH, null);
        } else {
            Toast.makeText(this, "Translate Text First",
                    Toast.LENGTH_SHORT).show();
        }

    }

    public void ExceptSpeechInput(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_input_phrase));

        try {
            startActivityForResult(intent, 100);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.stt_not_supported_message), Toast.LENGTH_LONG).show();
        }

    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if((requestCode == 100) && (data != null) &&
                (resultCode == RESULT_OK)) {
            ArrayList<String> spokenText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
             EditText wordsEntered = (EditText) findViewById(R.id.words_edit_text);

            wordsEntered.setText(spokenText.get(0));
        }
    }

    // Allows you to perform background operations without locking up the user interface
    // until they are finished
    // The void part is stating that it doesn't receive parameters, it doesn't monitor progress
    // and it won't pass a result to onPostExecute

    // First Void - this is not going to receive any parameters when its is called
    // Second Void - its not going to monitor the progress of the task being performed
    // Third Void - doInBackground is not going to pass anything to onPostExecute()
    private class GetXMLData extends AsyncTask<Void, Void, Void> {

        String stringToPrint = "";
        String wordsToTranslate = "";
        String xmlString = "";
        String xmlURL = "http://newjustin.com/translateit.php?action=xmltranslations&english_words=";

        @Override
        protected Void doInBackground(Void... params) {

            wordsToTranslate = etTranslate.getText().toString();

            wordsToTranslate = wordsToTranslate.replace(" ", "+");

            NetworkOperations();

            return null;
        }

        protected void NetworkOperations() {

            HttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());

            // this provides the URL for the post request
            HttpPost httpPost = new HttpPost(xmlURL + wordsToTranslate);

            httpPost.setHeader("Content-type", "text/xml");

            InputStream inputStream = null;

            try {
                HttpResponse response = httpClient.execute(httpPost);
                HttpEntity entity = response.getEntity();

                inputStream = entity.getContent();

                BufferedReader bf = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);

                StringBuilder sb = new StringBuilder();

                String line = "";

                while ((line = bf.readLine()) != null) {

                    sb.append(line);
                }

                xmlString = sb.toString();

                xmlParsing(xmlString);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        protected void xmlParsing(String xmlString) {
            try {

                // Generates an XML parser
                XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();

                // The XML parser that is generated will support XML namespaces
                xmlFactory.setNamespaceAware(true);

                // Initializing the XmlPullParser
                XmlPullParser xpp = null;

                // Gathers XML data and provides information on that data
                xpp = xmlFactory.newPullParser();

                // Input the XML data for parsing
                xpp.setInput(new StringReader(xmlString));

                // The event type is either START_DOCUMENT, END_DOCUMENT, START_TAG,
                // END_TAG, TEXT
                int eventType = xpp.getEventType();

                // Cycle through the XML document until the document ends
                while (eventType != XmlPullParser.END_DOCUMENT) {

                    // Each time you find a new opening tag the event type will be START_TAG
                    // We want to skip the first tag with the name translations
                    if ((eventType == XmlPullParser.START_TAG) && (!xpp.getName().equals("translations"))) {

                        // getName returns the name for the current element with focus
                        stringToPrint = stringToPrint + xpp.getName() + " : ";

                        // getText returns the text for the current event
                    } else if (eventType == XmlPullParser.TEXT) {
                        stringToPrint = stringToPrint + xpp.getText() + " \n ";
                    }

                    // next puts focus on the next element in the XML doc
                    eventType = xpp.next();
                }


            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            tvTranslation.setMovementMethod(new ScrollingMovementMethod());

            String translatedText = stringToPrint.replaceAll("\\w+\\s:", "#");

            arrayOfTranslations = translatedText.split("#");

            tvTranslation.setText(stringToPrint);
        }
    }
}
