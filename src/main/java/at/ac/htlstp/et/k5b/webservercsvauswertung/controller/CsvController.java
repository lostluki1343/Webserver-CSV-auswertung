package at.ac.htlstp.et.k5b.webservercsvauswertung.controller;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/")
public class CsvController {

    // Hier speichern wir das Dataset aus der CSV
    private XYSeriesCollection dataset = new XYSeriesCollection();

    // Zeigt das Upload-Formular an
    @GetMapping
    public String index() {
        return "index";
    }

    // Verarbeitet den Upload der CSV-Datei
    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            model.addAttribute("message", "Keine Datei hochgeladen!");
            return "index";
        }

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            // CSV parsen (hier wird angenommen, dass die CSV Header "x" und "y" hat)
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
            List<CSVRecord> records = csvParser.getRecords();

            XYSeries series = new XYSeries("CSV-Daten");
            for (CSVRecord record : records) {
                double x = Double.parseDouble(record.get("x"));
                double y = Double.parseDouble(record.get("y"));
                series.add(x, y);
            }
            // Altes Dataset entfernen und neues hinzufügen
            dataset.removeAllSeries();
            dataset.addSeries(series);

            csvParser.close();
            model.addAttribute("message", "CSV-Datei erfolgreich hochgeladen!");
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "Fehler beim Verarbeiten der Datei.");
        }
        return "index";
    }

    // Erzeugt den Plot als PNG-Bild und gibt ihn zurück
    @GetMapping("/plot")
    public ResponseEntity<byte[]> getPlot() {
        JFreeChart chart = ChartFactory.createXYLineChart(
                "CSV Plot", "X-Achse", "Y-Achse", dataset,
                PlotOrientation.VERTICAL, true, true, false);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(baos, chart, 800, 600);
            byte[] imageBytes = baos.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
