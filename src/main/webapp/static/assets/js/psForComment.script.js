/* Author Deepshikha Rajput */
function psvalues(){
	var psForComment =  $('option:selected', $("#psForComment")).val();
	var rbc_psc =  $("#rbc_psc");
	var wbc =  $("#wbc");
	var platelet =  $("#platelet");
	var parasite =  $("#parasite");
	var impression =  $("#impression");
	var toxic =  $("#toxic");
	var advice =  $("#advice");
	 if(psForComment == 'Normocytic Nomochromic'){
		 rbc_psc.val("Normocytic normochromic, nucleated RBCs absent/present."); 
			wbc.val("Within normal limit/increased/decreased, No immature cells seen, No toxic granules.");  
			platelet.val("Adequate/Increased/Decreased, Normal in morphology.");  
			parasite.val("No haemoparasite seen.");  
			impression.val("Normocytic normochromic blood picture.");  
			toxic.val("");  
			advice.val("");  
	 }	else if(psForComment == 'Mild microcytic'){
		 rbc_psc.val("Mild hypochromia with anisopoikilocytosis, Microcytes+, Normocytes+, Few pencil cells seen, nucleated RBCs absent/present."); 
			wbc.val("Within normal limit/increased/decreased, No immature cells seen, No toxic granules.");  
			platelet.val("Adequate/Increased/Decreased, Normal in morphology.");  
			parasite.val("No haemoparasite seen.");  
			impression.val("Microcytic hypochromic anaemia");  
			toxic.val("");  
			advice.val("Serum Iron & Ferritin Studies");  
		
	}else if(psForComment == 'Moderate Microcytic'){
		rbc_psc.val("Moderate hypochromia with anisopoikilocytosis, microcytes++, Normocytes+, Few pencil cells & Target cells seen, nucleated RBCs absent/present."); 
		wbc.val("Within normal limit/increased/decreased, No immature cells seen, No toxic granules.");  
		platelet.val("Adequate/Increased/Decreased, Normal in morphology.");   
		parasite.val("No haemoparasite seen.");  
		impression.val("Microcytic Hypochromic anaemia");  
		toxic.val(""); 
		advice.val("Serum Iron & Ferritin Studies.");  
		
	}else if(psForComment == 'Severe Microcytic'){
		 
		rbc_psc.val("Severe hypochromia with anisopoikilocytosis, Microcytes+++, Fair number of pencil cells, target cells & tear drop cells seen, nucleated RBCs absent/present."); 
		wbc.val("Within normal limit/increased/decreased, No immature cells seen, No toxic granules.");  
		platelet.val("Adequate/Increased/Decreased, Normal in morphology.");    
		parasite.val("No Hemoparasite Seen.");  
		impression.val("Microcytic Hypochromic anaemia");  
		toxic.val(""); 
		advice.val("Serum Iron & Ferritin Studies.");
	}else if(psForComment == 'Macrocytic'){
		rbc_psc.val("Macrocytic with anisopoikilocytosis, normochromic, Macroovalocyte, tear drop cells, Howel jolly bodies, Late megaloblast+."); 
		wbc.val("Leucopenia/ within normal limit, Hypersegmented neutrophils+, No toxic granules, No immature cells seen.");  
		platelet.val("Thrombocytopenia/within normal limit, Normal in morphology.");  
		parasite.val("No haemoparasite seen.");  
		impression.val("Megaloblastic anaemia.");  
		toxic.val("");  
		advice.val("Serum Vit B12 & Folic Acid levels."); 
		
	}else if(psForComment == 'Dimorphic Microcytic'){
		rbc_psc.val("Mild to moderate hypochromia with anisopoikilocytosis, Microcyte++, Macrocytes+, Few pencil cells, Target cells, Polychromatic cells seen."); 
		wbc.val("Within normal limit/increased/decreased, No immature cells seen, No toxic granules.");  
		platelet.val("Adequate/Increased/Decreased, Normal in morphology.");  
		parasite.val("No haemoparasite seen.");  
		impression.val("Dimorphic anaemia.");  
		toxic.val("");  
		advice.val("Serum Iron, Ferritin, B12, Folic acid level.");  
		
	}else if(psForComment == 'Dimorphic Macrocytic1'){
		rbc_psc.val("Microcyte++, Macrocytes+, Few pencil cells, Target cells, Polychromatic cells seen."); 
		wbc.val("Within normal limit/increased/decreased, No immature cells seen, No toxic granules.");  
		platelet.val("Adequate/Increased/Decreased, Normal in morphology.");  
		parasite.val("No haemoparasite seen.");  
		impression.val("Dimorphic anaemia.");  
		toxic.val(""); 
		advice.val("Serum Iron, Ferritin, B12, Folic acid level.");  
		
	}else if(psForComment == 'Haemolytic Anaemia'){
		rbc_psc.val("Normocytic Normochromic/Microcytic Hypochromic with anisopoikilocytosis, Spherocytes/Schistocytes/sickle cells, Polychromatic cells, tear drop cells, Howel jolly bodies, Late erythroblast+."); 
		wbc.val("Increased, Neutrophilia with shift to left.");  
		platelet.val("Increased, thrombocytosis giant platelets+.");  
		parasite.val("No haemoparasite seen.");  
		impression.val("Haemolytic anaemia.");  
		toxic.val(""); 
		advice.val("Hb electrophoresis");  
		
	}else if(psForComment == 'Pancytopenia'){
		rbc_psc.val("Normocytic nomochromic, Normoblast absent."); 
		wbc.val("Count reduced, Neutrophils reduced, No immature cells, No toxic granules.");  
		platelet.val("Count reduced, Normal in morphology.");  
		parasite.val("No haemoparasite seen.");  
		impression.val("Pancytopenia.");  
		toxic.val(""); 
		advice.val("Bone marrow aspiration cytology");  
		
	}else if(psForComment == 'Acute Leukemia ALL'){
		rbc_psc.val("Normocytic nomochromic, Nucleated RBC+."); 
		wbc.val("Count normal/increased/decreased, Lymphoblast+, L1/L2/L3 Type(40-95%), Neutropenia.");  
		platelet.val("Decreased, Normal in morphology.");  
		parasite.val("No haemoparasite seen.");  
		impression.val("Acute lymphoblastic Leukemia L1/L2/L3 type.");  
		toxic.val(""); 
		advice.val("Bone marrow aspiration cytology, cytochemistry cell marker study");  
		
	}else if(psForComment == 'Acute Leukemia AML'){
		rbc_psc.val("Normocytic Nomochromic, Nucleated RBC+."); 
		wbc.val("Count normal/increased/decreased, Myeloblast+/Monoblast+, Promyelocyte/Promonocyte+, Hypogranular myelocyte+, Metamyelocyte+.");  
		platelet.val("Decreased.");  
		parasite.val("No haemoparasite seen.");  
		impression.val("Acute Myeloblastic Leukemia.");  
		toxic.val(""); 
		advice.val("Bone marrow aspiration cytology, cytochemistry cell marker study");  
		
	}else if(psForComment == 'Chronic myeloid Leukemia'){
		rbc_psc.val("Normocytic nomochromic, Nucleated RBC+."); 
		wbc.val("Marked Leucocytosis, Myeloblast++, Promyelocyte+, myelocyte++, Metamyelocyte+++, Neutrophils++, Basophilia/Eosinophilia(5-15%).");  
		platelet.val("Normal/Thrombocytosis.");  
		parasite.val("No haemoparasite seen.");  
		impression.val("Chronic myeloid  Leukemia chronic phase blast –5%, Chronic myeloid  Leukemia chronic Accelerated Phase blast + 10-20%, Chronic myeloid  Leukemia blastic Phase, Myeloblast>20%, Lymphoblast>30%, Basophils>20%.");  
		toxic.val(""); 
		advice.val("");  
		
	}else if(psForComment == 'Chronic Lymphocytic Leukemia'){
		rbc_psc.val("Normocytic nomochromic, Nucleated RBC+."); 
		wbc.val("Lymphocytosis 50-98%, Mature lymphocyte++, Mature lymphocytes with notched nucleus+, Smudge cells+.");  
		platelet.val("Normal/decreased.");  
		parasite.val("No haemoparasite seen.");  
		impression.val("Chronic Lymphocytic  Leukemia.");  
		toxic.val(""); 
		advice.val("");  
		
	}else if(psForComment == 'Multiple myeloma'){
		rbc_psc.val("Normocytic nomochromic, Marked rouleax formation, bluish background."); 
		wbc.val("Total count is within normal limit, Mature Plasma cells+, Immature Plasma Cells+, Multinucleated Plasma cells+.");  
		platelet.val("Adequate/Increased/Decreased, Normal in morphology..");  
		parasite.val("No haemoparasite seen.");  
		impression.val("Plasma Cell Leukemia/Multiple myeloma.");  
		toxic.val(""); 
		advice.val("");  
		
	}else{
		rbc_psc.val(""); 
		wbc.val("");  
		platelet.val("");  
		parasite.val("");  
		impression.val("");  
		toxic.val("");  
		advice.val("");  
		
	}
	
	
}
