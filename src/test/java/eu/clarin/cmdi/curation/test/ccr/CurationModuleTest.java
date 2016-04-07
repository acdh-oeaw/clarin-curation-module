package eu.clarin.cmdi.curation.test.ccr;

import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.clarin.cmdi.curation.cr.CRService;
import eu.clarin.cmdi.curation.cr.ProfileDescriptions.ProfileHeader;
import eu.clarin.cmdi.curation.main.Configuration;
import eu.clarin.cmdi.curation.main.CurationModule;
import eu.clarin.cmdi.curation.main.CurationModuleInterface;
import eu.clarin.cmdi.curation.report.CMDProfileReport;
import eu.clarin.cmdi.curation.report.Report;

public class CurationModuleTest {

	static CurationModuleInterface module;

	public static void main(String[] args) throws Exception {

		Configuration.initDefault();
		Configuration.OUTPUT_DIRECTORY = null;
		module = new CurationModule();
		test();

	}

	public static void test() throws Exception {
		Path instancePath1 = FileSystems.getDefault().getPath("D:/data/cmdi/Deutsches_Textarchiv/dta_386.xml");
		Path instancePath2 = FileSystems.getDefault()
				.getPath("D:/data/cmdi/BAS_Repository/oai_BAS_repo_Corpora_aGender_101106.xml");

		URL instanceURL = new URL(
				"https://vlo.clarin.eu/data/clarin/results/cmdi/CLARIN_DK_UCPH_Repository/oai_clarin_dk_dkclarin_1257002.xml");

		Path cmdi = FileSystems.getDefault().getPath("D:/data/cmdi");
		Path test = FileSystems.getDefault().getPath("D:/data/test/test1");

		Path ehu_18 = FileSystems.getDefault().getPath("D:/data/cmdi/Euskal_Herriko_Unibertsitatea");
		Path lbof_295 = FileSystems.getDefault().getPath("D:/data/cmdi/Language_Bank_of_Finland");
		Path eloftw_7K = FileSystems.getDefault().getPath("D:/data/cmdi/Ethnologue_Languages_of_the_World");
		Path bas_23K = FileSystems.getDefault()
				.getPath("D:/data/cmdi/BAS_Repository/oai_BAS_repo_Corpora_aGender_101104.xml");
		Path unname_65K = FileSystems.getDefault().getPath("D:/data/cmdi/Unnamed_provider_at_dspace_library_uu_nl");
		Path mee_240K = FileSystems.getDefault().getPath("D:/data/cmdi/Meertens_Institute_Metadata_Repository");

		URL instanceURL1 = new URL(
				"https://vlo.clarin.eu/data/clarin/results/cmdi/CLARIN_DK_UCPH_Repository/oai_clarin_dk_dkclarin_1257001.xml");

		URL profileURL1 = new URL(
				"http://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/profiles/clarin.eu:cr1:p_1324638957718");
		URL profileURL2 = new URL(
				"http://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/profiles/clarin.eu:cr1:p_1407745711934");

		Report r = null;

		r = module.processCMDProfile("clarin.eu:cr1:p_1407745711934");

		// profile is not public
		// r = module.processCMDProfile("clarin.eu:cr1:p_1369140737154");

		// r = module.processCMDProfile(profileURL2);

		// profile is not public
		// r = module.processCMDInstance(instancePath1);

		// r = module.processCMDInstance(instanceURL1);

		// r = module.processCollection(ehu_18);

		r.toXML(System.out);


	}

	static public void nonCoverageForPublicProfiles() throws Exception {

		List<CMDProfileReport> profiles = CRService.getInstance()
				.getPublicProfiles()
				.parallelStream()
				.map(profile -> (CMDProfileReport) module.processCMDProfile(profile.getId()))
				.collect(Collectors.toList());

		for (CMDProfileReport report : profiles) {
			System.out.println("ID: " + report.header.ID + ", non-coverage: " + report.facet.profile.notCovered.size() + "/"
					+ report.facet.numOfFacets);

			for (String facet : report.facet.profile.notCovered)
				System.out.println(facet);
			System.out.println("--------------------------------------");

		}
	}
	
	public static void profilesWithDuplicatedName() throws Exception{
Map<String, Integer> profileMap = new HashMap<>();
		
		List<ProfileHeader> profiles = CRService.getInstance().getPublicProfiles();

		for (ProfileHeader p : profiles) {
			String profName = p.getName();
			if (profileMap.containsKey(profName))
				profileMap.put(profName, profileMap.get(profName) + 1);
			else
				profileMap.put(profName, 1);
		}

		for (Map.Entry entry : profileMap.entrySet()) {
			if((int)entry.getValue() > 1)
			System.out.println(entry.getKey() + ", " + entry.getValue());
		}
	}

}
