package cybercubeChallenge.listener;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.SoftAssertionError;
import org.testng.*;
import org.testng.xml.XmlSuite;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Log4j
public class ExtentTestNGIReporterListener implements IReporter {

	private static final String OUTPUT_FOLDER = "test-output/";
	private static final String FILE_NAME = "Extent.html";
	private ExtentReports extent;
	private Map<String,String> knownFailures;
	private List<String> softAssertScreenshots;
	private List<String> softAssertFailureIds;
	private static boolean isNewReportFormat = true;
	private static final String PDF_VALIDATION_GUIDE="<b>How to interpret PDF Visual validation:" +
			"</b><br><ul><li>Pixels that are equal are faded a bit.</li><li>Pixels that differ are marked in red and green. " +
			"Red for pixels that where expected, but didn't come. Green for pixels that are there, but where not expected.</li>" +
			"<li> Markings at the edge of the paper in magenta to find areas that differ quickly.</li><li> Ignored Areas are marked" +
			" with a yellow background.</li><li> Pages that where expected, but did not come are marked with a red border.</l>" +
			"<li> Pages that appear, but where not expected are marked with a green border.</li></ul>";

	@Override
	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
		readKnownFailuresFromSuite(xmlSuites);
		init(suites);
		if (isNewReportFormat) {
			reportMultiSuite(suites);
		} else {
			singleSuiteReporting(suites, null);
		}
		for (String s : Reporter.getOutput()) {
			extent.setTestRunnerOutput(s);
		}
		extent.flush();
	}

	private void reportMultiSuite(List<ISuite> suites) {
		suites.forEach(s -> {
			if (s.getResults().isEmpty()) {
				return;
			}
			String category = null;
			if (!isParentSuite(s)) {
				category = s.getName();
			}
			singleSuiteReporting(List.of(s), category);
		});
	}

	public void singleSuiteReporting(List<ISuite> suites, String category) {
		for (ISuite suite : suites) {
			Map<String, ISuiteResult> result = suite.getResults();
			for (ISuiteResult r : result.values()) {
				ITestContext context = r.getTestContext();
				buildTestNodes(context.getFailedTests(), category, Status.FAIL);
				buildTestNodes(context.getSkippedTests(), category, Status.SKIP);
				buildTestNodes(context.getPassedTests(), category, Status.PASS);
				buildTestNodes(context.getFailedConfigurations(),category,Status.FAIL);
			}
		}
		for (String s : Reporter.getOutput()) {
			extent.setTestRunnerOutput(s);
		}
		extent.flush();
	}

	private boolean isParentSuite(ISuite suite) {
		return suite.getXmlSuite().getParentSuite() == null;
	}

	private void init(List<ISuite> suites) {
		createTestOutPutFolder();
		String parentSuiteName = null;
		for(ISuite suite : suites) {
			if (isParentSuite(suite)) {
				parentSuiteName = suite.getXmlSuite().getName();
				break;
			}
		}
		ExtentSparkReporter htmlReporter = new ExtentSparkReporter(OUTPUT_FOLDER + FILE_NAME);
		htmlReporter.config().setDocumentTitle("ExtentReports : Created by TestNG Listener");
		htmlReporter.config().setReportName("ExtentReports : " + parentSuiteName);
		htmlReporter.config().setTheme(Theme.STANDARD);
		htmlReporter.config().enableTimeline(false);
		extent = new ExtentReports();
		extent.attachReporter(htmlReporter);

		extent.setReportUsesManualConfiguration(true);
	}

	private void buildTestNodes(IResultMap tests, String category, Status status) {
		ExtentTest test;
		if (tests.size() > 0) {
			// Sort the results based on method name
			List<ITestResult> sortedTestList = tests.getAllResults().stream()
					.sorted(Comparator.comparing(e -> e.getMethod().getMethodName())).collect(Collectors.toList());
			for (final ITestResult result : sortedTestList) {
				softAssertScreenshots = new ArrayList<>();
				softAssertFailureIds = new ArrayList<>();
				findSoftAssertFailureIds(result.getThrowable());
				if (result.getMethod().getDescription() != null && !result.getMethod().getDescription().isEmpty()) {
					String[] jiraIds = result.getMethod().getDescription().substring(result.getMethod().getDescription().indexOf("#") + 1).split(",");
					String hyperLink = "";
					for (String jiraId : jiraIds) {
						String jiraUrl = String.format("https://.atlassian.net/browse/%s", jiraId);
						hyperLink = hyperLink.concat(String.format("<a href='%s' target=\"_blank\">%s</a> ", jiraUrl, jiraId));

						if (jiraId.trim().length() > 1) {
							extent.setSystemInfo("<a href=\"https://.atlassian.net/browse/" + jiraId + "\">" + jiraId + "</a>", status.toString());
						}
					}
					String allExpectedFailures = isAllSoftAssertionsExpected() ? " <span class=\"label start-time\">Expected Failures</span>" : "";
					test = extent.createTest(String.format("%s : %s %s", result.getMethod().getMethodName(), hyperLink, allExpectedFailures));
				} else {
					test = extent.createTest(result.getMethod().getMethodName());
				}
				test.assignCategory(category);
				log.info(String.format("Assigned category [%s] to test [%s]", category, result.getMethod().getMethodName()));
				//Used for retrieving the screenshots and the video
				String testFolderName = result.getMethod().getMethodName();
				if (result.getParameters() != null && result.getParameters().length > 0) {
					testFolderName = String.format("%s_withParameters_%s", result.getMethod().getMethodName(),
							result.getAttribute("StartTimeStamp"));

					Object[] parameters = result.getParameters();
					ArrayList<Object> arrayParam = new ArrayList<Object>(Arrays.asList(parameters));
					String paramsCommaSeparated = arrayParam.stream()
							.map(param -> param == null ? "null" : param.toString())
							.collect(Collectors.joining(","));
					test.info("<br><b>Parameters:</b> ".concat(paramsCommaSeparated));
				}
				for (String group : result.getMethod().getGroups())
					test.assignCategory(group);

				Throwable resultException = result.getThrowable();
				if (resultException == null) {
					test.log(status, "Test " + status.toString().toLowerCase() + "ed");
				} else {
					if (resultException instanceof SoftAssertionError) {
						addSoftAssertionFailuresForTest(test, resultException);
					} else {
						test.log(status, result.getThrowable());
						addFailureInfoForTest(test, result.getThrowable());
					}
				}
				
				Object webTest = result.getAttribute("WebTest");
				test.getModel().setStartTime(getTime(result.getStartMillis()));
				test.getModel().setEndTime(getTime(result.getEndMillis()));
			}
		}
	}

	private void createTestOutPutFolder() {
		File directory = new File("test-output");
		if (! directory.exists()){
			directory.mkdir();
		}
	}

	private Date getTime(long millis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millis);
		return calendar.getTime();      
	}

	private String getTestFailureId(Throwable th){
		String fId = "";
		for (StackTraceElement te: th.getStackTrace()){
			if (te.getClassName().contains("saetabis.automation.testingWrapper.tests")){
				fId = fId.concat(te.getMethodName() + ":" + String.valueOf(te.getLineNumber()));
			}
		}
		return fId;
	}

	private void readKnownFailuresFromSuite(List<XmlSuite> xmlSuites){
		String failures = xmlSuites.get(0).getParameter("knownFailure");
		if (failures!=null && !failures.isEmpty()){
			knownFailures = Pattern.compile("\\s*;\\s*")
					.splitAsStream(failures.trim())
					.map(s -> s.split("_", 2))
					.collect(Collectors.toMap(a -> a[0], a -> a.length>1? a[1]: ""));
		}
	}

	private boolean isAllSoftAssertionsExpected() {
		if (knownFailures == null || knownFailures.isEmpty() || softAssertFailureIds == null || softAssertFailureIds.isEmpty())
			return false;
		return knownFailures.keySet().containsAll(softAssertFailureIds);
	}

	private void addFailureInfoForTest(ExtentTest test,Throwable th){
		String testFailId = getTestFailureId(th);
		String infoMsg ="";
		if (knownFailures!=null && !knownFailures.isEmpty() && knownFailures.get(testFailId)!=null){
			String jiraUrl = String.format("https://.atlassian.net/browse/%s", knownFailures.get(testFailId));
			String hyperLink = String.format("<a style=\"color:#fff;\" href='%s'>%s</a> ", jiraUrl, knownFailures.get(testFailId));
			infoMsg = "<b style=\"background-color:#00c853;\">Failure is already reported: " + hyperLink + " </b>";
		}
		test.info(String.format("<b style=\"background-color:#FFFACD;\">Checkpoint ID: %s </b><br> %s",testFailId,infoMsg));
	}

	private void findSoftAssertFailureIds(Throwable resultThrowable) {
		if (resultThrowable instanceof SoftAssertionError) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			for (String error : ((SoftAssertionError) resultThrowable).getErrors()) {
				try {
					// Read exception and create a Throwable from
					Throwable th = mapper.readValue(error, Throwable.class);
					softAssertFailureIds.add(getTestFailureId(th) + "#" + StringUtils.substringBetween(th.getMessage().replace(" ", ""), "[", "]"));

				} catch (IOException e) {
					log.info("Failed to create the list of soft assertion failure ids", e);
				}
			}
		}
	}

	private void addSoftAssertionFailuresForTest(ExtentTest test, Throwable resultThrowable ){
		List<String> errors=new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		for (String error:((SoftAssertionError) resultThrowable).getErrors()){
			try {
				// Read exception and create a Throwable from
				Throwable th = mapper.readValue(error,Throwable.class);
				// create screeenshot names
				String screenShotFileName = null;
				for (int i=0;i<th.getStackTrace().length;i++){
					if (th.getStackTrace()[i].getClassName().contains("saetabis.automation.testingWrapper")){
						if (screenShotFileName == null) {
							screenShotFileName = th.getStackTrace()[i].getMethodName();
						}
						else {
							screenShotFileName = screenShotFileName + "-" + String.valueOf(th.getStackTrace()[i].getLineNumber());
						}
					}else{
						continue;
					}
				}
				softAssertScreenshots.add(screenShotFileName.concat("_screenshot.png"));
				//Five level should be enough to get us to the test class
				StringBuilder stLog = new StringBuilder();
				stLog.append(th.getMessage());
				StackTraceElement[] stElements = th.getStackTrace();
				for (int i = 0; i < 5; i++) {
					stLog.append(System.lineSeparator());
					stLog.append(stElements[i].toString());
				}
				errors.add(stLog.toString());
			} catch (IOException e) {
				log.info("Failed to create the list of screenshots for soft assertion failures",e);
			}
		}
		test.log(Status.ERROR, new SoftAssertionError(errors));
	}

	private void addScreenshotsForTest(ExtentTest test,ITestResult result,String testFolderName){
		try {
			Iterator it = FileUtils.iterateFiles(new File("test-output/screenshot_downloads/" + testFolderName + "/"), null, false);
			boolean pdfVisualValidation = false;
			while (it.hasNext()) {
				String fileName = ((File) it.next()).getName();
				if (fileName.contains("diff") || fileName.contains(result.getMethod().getMethodName()) || softAssertScreenshots.contains(fileName)) {
					int[] indices = IntStream
							.range(0, softAssertScreenshots.size())
							.filter(i -> softAssertScreenshots.get(i).equalsIgnoreCase(fileName))
							.toArray();
					if (indices.length != 0) {
						for (int screenshotIndex : indices) {
							String infoMsg = "";
							if (knownFailures!=null && !knownFailures.isEmpty() && softAssertFailureIds!=null && !softAssertFailureIds.isEmpty() && knownFailures.get(softAssertFailureIds.get(screenshotIndex))!=null){
								String jiraUrl = String.format("https://.atlassian.net/browse/%s", knownFailures.get(softAssertFailureIds.get(screenshotIndex)));
								String hyperLink = String.format("<a style=\"color:#fff;\" href='%s'>%s</a> ", jiraUrl, knownFailures.get(softAssertFailureIds.get(screenshotIndex)));
								infoMsg = "<br><b style=\"background-color:#00c853;\">Failure is already reported: " + hyperLink + " </b>";
							}
							if (softAssertFailureIds != null && !softAssertFailureIds.isEmpty()) {
								test.warning(String.format("<b>Failure %s </b><br><b style=\"background-color:#FFFACD;\">Checkpoint ID: %s</b> %s",
										screenshotIndex + 1,
										softAssertFailureIds.get(screenshotIndex),
										infoMsg),
										MediaEntityBuilder.createScreenCaptureFromPath("screenshot_downloads/" + testFolderName + "/" + fileName).build());
							}
						}
					} else {
						if (fileName.contains("pdf")) {
							pdfVisualValidation = true;
							test.warning(String.format("<a style=\"color:#FF0000;\" href='%s'>Download %s</a> ", "screenshot_downloads/" + testFolderName + "/" + fileName, fileName));
						} else {
							test.warning(fileName, MediaEntityBuilder.createScreenCaptureFromPath("screenshot_downloads/" + testFolderName + "/" + fileName).build());
						}
					}
				}
			}
			if (pdfVisualValidation){
				test.info(PDF_VALIDATION_GUIDE);
			}
		} catch (IOException | IllegalArgumentException e) {
			log.error("Failed to add the screenshot failure to the report",e);
		}
	}
}