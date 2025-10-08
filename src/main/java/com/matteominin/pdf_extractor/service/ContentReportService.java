package com.matteominin.pdf_extractor.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matteominin.pdf_extractor.model.content.Architecture;
import com.matteominin.pdf_extractor.model.content.ContentReport;
import com.matteominin.pdf_extractor.model.content.Requirement;
import com.matteominin.pdf_extractor.model.content.Test;
import com.matteominin.pdf_extractor.model.content.UseCase;

@Service
public class ContentReportService {
	@Autowired
	private EmbeddingService embeddingService;

	public ContentReport consolidateReport(List<UseCase> useCases, List<Requirement> requirements,
			List<Architecture> architectures, List<Test> tests) {

		List<UseCase> consolidatedUseCases = consolidate(useCases);
		List<Requirement> consolidatedRequirements = consolidate(requirements);
		List<Test> consolidatedTests = tests; // TODO: consolidate(tests) when available

		ContentReport report = ContentReport.builder()
				.useCases(consolidatedUseCases)
				.requirements(consolidatedRequirements)
				.architectures(architectures)
				.tests(consolidatedTests)
				.build();

		return report;
	}

	private <T> List<T> consolidate(List<T> items) {
		// Set progressive IDs
		for (int i = 0; i < items.size(); i++) {
			Object item = items.get(i);
			if (item instanceof Requirement) {
				((Requirement) item).setReqId("REQ-" + (i + 1));
			} else if (item instanceof UseCase) {
				((UseCase) item).setCaseId("UC-" + (i + 1));
			} else if (item instanceof Test) {
				((Test) item).setTestId("TEST-" + (i + 1));
			}
		}

		List<String> descriptions = items.stream()
				.map(item -> item.toString())
				.toList();

		List<List<Double>> embeddings = embeddingService.generateEmbedding(descriptions);

		List<String> ids = items.stream()
				.map(item -> {
					if (item instanceof Requirement) {
						return ((Requirement) item).getReqId();
					} else if (item instanceof UseCase) {
						return ((UseCase) item).getCaseId();
					} else if (item instanceof Test) {
						return ((Test) item).getTestId();
					} else {
						return null;
					}
				})
				.toList();

		List<List<String>> clusteredIds = ClusteringService.pairwiseClusteringWithCosineSimilarity(ids,
				embeddingService.convertToDoubleMatrix(embeddings), 0.92);

		List<T> consolidatedItems = clusteredIds.stream()
				.map(cluster -> items.stream()
						.filter(item -> {
							if (item instanceof Requirement) {
								return ((Requirement) item).getReqId().equals(cluster.get(0));
							} else if (item instanceof UseCase) {
								return ((UseCase) item).getCaseId().equals(cluster.get(0));
							} else if (item instanceof Test) {
								return ((Test) item).getTestId().equals(cluster.get(0));
							} else {
								return false;
							}
						})
						.findFirst()
						.orElse(null))
				.toList();

		for (int i = 0; i < consolidatedItems.size(); i++) {
			Object item = consolidatedItems.get(i);
			if (item instanceof Requirement) {
				((Requirement) item).setReqId("REQ-" + (i + 1));
			} else if (item instanceof UseCase) {
				((UseCase) item).setCaseId("UC-" + (i + 1));
			} else if (item instanceof Test) {
				((Test) item).setTestId("TEST-" + (i + 1));
			}
		}
		return consolidatedItems;
	}
}
