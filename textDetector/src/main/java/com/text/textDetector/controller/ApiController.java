package com.text.textDetector.controller;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.ImageSource;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.cloud.vision.v1.Word;
import com.google.common.collect.Lists;

@RestController
public class ApiController {

	

	@RequestMapping(value = "/fetchTextFromFile", method = { RequestMethod.POST })
	public void detectDocumentTextGcs(@RequestBody String text) throws IOException {
		
String gcsPath = "gs://gcpdocstorage/dummy.pdf";
		List<AnnotateImageRequest> requests = new ArrayList<>();

//		GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream("C:\\Users\\Sahil Aggarwal\\Desktop\\textDetector\\textDetector\\src\\main\\resources\\My First Project-5fcc280ffe7a.json"))
//				.createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
		ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
		Image img = Image.newBuilder().setSource(imgSource).build();
		Feature feat = Feature.newBuilder().setType(Type.DOCUMENT_TEXT_DETECTION).build();
		AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
		requests.add(request);

		
//		ImageAnnotatorSettings imageAnnotatorSettings =
//			     ImageAnnotatorSettings.newBuilder()
//			         .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
//			         .build();
		try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
			BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();
			client.close();

			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					System.out.format("Error: %s%n", res.getError().getMessage());
					return;
				}
				TextAnnotation annotation = res.getFullTextAnnotation();
				for (Page page : annotation.getPagesList()) {
					String pageText = "";
					for (Block block : page.getBlocksList()) {
						String blockText = "";
						for (Paragraph para : block.getParagraphsList()) {
							String paraText = "";
							for (Word word : para.getWordsList()) {
								String wordText = "";
								for (Symbol symbol : word.getSymbolsList()) {
									wordText = wordText + symbol.getText();
									System.out.format("Symbol text: %s (confidence: %f)%n", symbol.getText(),
											symbol.getConfidence());
								}
								System.out.format("Word text: %s (confidence: %f)%n%n", wordText, word.getConfidence());
								paraText = String.format("%s %s", paraText, wordText);
							}
							System.out.println("%nParagraph: %n" + paraText);
							System.out.format("Paragraph Confidence: %f%n", para.getConfidence());
							blockText = blockText + paraText;
						}
						pageText = pageText + blockText;
					}
				}
				System.out.println("%nComplete annotation:");
				System.out.println(annotation.getText());
			}
		}
	}
}
