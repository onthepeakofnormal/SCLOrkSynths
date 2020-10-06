
SCLOrkSynths {

	classvar <window, bankPathsArray, patternPathsArray, <synthDictionary, <folderPath;

	// add all SynthDefs (checks if server is on)
	// optional: force reboot server to ensure right memSize and stuff?

	*initClass {
		/*
		Creates a Dictionary of Dictionaries holding relevant info about each synth:
		Dictionary[
		  (synthName1 ->
		     Dictionary[
		      (bank -> bank),
		      (synthPath -> path),
		      (patternPath -> path)
		     ],
		  ),
		  (synthName2 ->
		     Dictionary[
		       (bank -> bank),
		       (synthPath -> path),
		       (patternPath -> path)
		     ],
		   ),
		... etc
		]
		*/

		// Check for right path of SCLOrkSynths folder
		Quarks.installedPaths.do({ |p|
			var path = PathName.new(p);
			if( path.fileName.asString == "SCLOrkSynths", {
				folderPath = path.fullPath;
			})
		});

		// Construct main dictionary
		SCLOrkSynths.prCreateMainDict;

	}

	*prCreateMainDict {

		// list of bank PathNames -- one for each bank folder
		bankPathsArray = PathName.new(folderPath ++ "/SynthDefs").folders;

		// list of pattern PathNames -- one for each pbind file
		patternPathsArray = PathName.new(folderPath ++ "/pbind-demos").files;

		// this is the single big dictionary
		synthDictionary = Dictionary.new;

		// iterate through list of bank PathNames
		bankPathsArray.do({ arg bankPath;
			var bankName = bankPath.folderName;

			// iterate over array of file Paths in bankPath to
			// populate inner Dictionaries, one for each synth.

			bankPath.files.do({ arg synthPath;
				var synthName = synthPath.fileNameWithoutExtension.asSymbol;
				var patternPath;

				// find path for the corresponding pattern
				patternPathsArray.do({ arg p;
					var pName = p.fileNameWithoutExtension.drop(-5).asSymbol;
					if(pName === synthName, { patternPath = p.fullPath });
				});

				// add everything in:
				synthDictionary.add(
					synthName -> Dictionary.newFrom(
						[
							\synthPath, synthPath.fullPath,
							\bank, bankPath.folderName,
							\patternPath, patternPath
						]
					)
				)
			});
		});


	}

	// Load all SynthDefs and Pbindefs:
	*load {

		Server.default.waitForBoot({
			synthDictionary.keysValuesDo({ arg synthName, synthDic;
				synthDic[\patternPath].postln;
				synthDic[\patternPath].asString.load;
				synthDic[\synthPath].postln;
				synthDic[\synthPath].asString.load;
			});
			"synthDictionary was created.".postln;
			"all SynthDefs and Patterns were loaded".postln;
			" "
		});
	}

	// Create GUI
	*gui {

		var header;
		var gap = 5;
		var margin = 10;
		var banksMenu;
		var currentBank = \drums;
		var numberOfColumns = 5;
		var numberOfRows = 10;
		var windowWidth = 800;
		var windowHeight = 480;
		var buttonWidth = windowWidth - (margin * 2) - (numberOfColumns * (gap - 1)) / numberOfColumns;
		var buttonHeight = 25; //(windowHeight * 0.75) / numberOfRows;
		var buttonArray;
		// var bankNameArray = (Document.current.dir ++ "/*.scd").resolveRelative.pathMatch.collect({ arg path; path.basename.drop(-4).asSymbol });
		var activeButton;
		var footer1, footer2;
		var currentSynth;
		var currentSynthText;

		// might move this out of here as a separate listBanks method
		var banks = PathName.new(folderPath +/+ "SynthDefs").folders.collect({ arg p; p.folderName.asSymbol});

		// Load all SynthDefs and Patterns

		if( window.isNil, {

			SCLOrkSynths.load;

			window = Window.new(
				name: "SCLOrkSynths",
				bounds: Rect.new(
					left: 100,
					top: 100,
					width: windowWidth,
					height: windowHeight
				),
				resizable: false
			);

			window.front;

			window.view.decorator = FlowLayout.new(
				bounds: window.view.bounds,
				margin: Point.new(margin, margin),
				gap: Point.new(gap, gap)
			);

			// header is just the area where drop down menu sits
			header = CompositeView.new(window, Rect.new(0, 0, windowWidth - (margin * 2), 50));

			// StaticText goes first so EZPopUpMenu stays on top
			StaticText.new(
				parent: header,
				bounds: Rect(0, 0, header.bounds.width, header.bounds.height))
			.string_("SCLOrkSynths")
			// .background_(Color.green(0.5, 0.2))
			.align_(\topRight)
			.font_(Font(Font.default, size: 24, bold: true));

			banksMenu = EZPopUpMenu.new(
				parentView: header,
				bounds: Rect.new(0, 10, 185, 30),
				label: "bank: ",
				items: banks,
				globalAction: { arg menu;
					var count = 0;
					// ["bank menu action", menu.value, menu.item].postln;
					currentBank = menu.item; // currentBank holds a symbol, not a number
					// clean up buttons
					buttonArray.do({arg button;
						button.string = " ";
					});
					currentBank.postln;

					// kind of works, but not alphabetical -- fix that later
					synthDictionary.keys.asArray.sort.do({ arg synthName;
						var synthDic = synthDictionary[synthName.asSymbol];
						var indexDownByColumn = count % numberOfRows * numberOfColumns + count.div(numberOfRows);

						// synthName.postln;

						if(synthDic[\bank].asSymbol===currentBank.asSymbol,
							{
								buttonArray[indexDownByColumn].string = synthName.asString;
								count = count + 1;
							}
						)
					});
				},
				initVal: banks.indexOf(currentBank.asSymbol),
				initAction: false, // because buttonArray does not exist yet
				labelWidth: 50
			);


			// header.background = Color.rand;

			buttonArray = 50.collect({ arg count;
				Button.new(
					parent: window.view,
					bounds: Point.new(buttonWidth, buttonHeight),
				)
				.action_({ arg button;
					currentSynthText.string = button.string;
					currentSynth = button.string;
				});
			});

			// now that buttonArray exists, we can run EZPopUpMenu action to initialize button labels:
			banksMenu.valueAction = currentBank;

			footer1 = CompositeView.new(window, Rect.new(0, 300, windowWidth - (margin * 2), 50));
			// footer1.background = Color.green(0.5, 0.2);

			// footer1.bounds.height.postln;

			currentSynthText = StaticText.new(
				parent: footer1,
				bounds: Rect(0, 0, footer1.bounds.width, footer1.bounds.height))
			.string_("click on a button to choose a SynthDef")
			.background_(Color.gray(0.5, 0.2))
			.align_(\center)
			.font_(Font(Font.default, size: 24, bold: true))
			.front;

			// empty space
			/* StaticText.new(
			parent: window,
			bounds: Rect.new(0, 300, windowWidth / 3 - (margin * 2), 50)
			);
			*/

			footer2 = CompositeView.new(window, Rect.new(0, 0, windowWidth - (margin * 2), 50));

			// placeholder button
			Button.new(
				parent: footer2,
				bounds: Rect.new(
					left: 0,
					top: 0,
					width: footer2.bounds.width / 9 * 2,
					height: 50
				)
			)
			.string_("open more demos")
			// .font_(Font(Font.default.name, 18))
			.action_({ arg button;
				// button.value.postln;
				SCLOrkSynths.prOpenMoreDemos(currentSynth);
			})
			.front;

			// play button
			Button.new(
				parent: footer2,
				bounds: Rect.new(
					left: footer2.bounds.width / 3,
					top: 0,
					width: footer2.bounds.width / 3,
					height: 50
				)
			)
			// .string_("play demo")
			.states_([
				["play demo", Color.black, Color.green],
				["stop", Color.white, Color.red]
			])
			.font_(Font(Font.default.name, 18))
			.action_({ arg button;
				// button.value.postln;
				if((button.value==1),
					{
						Pdef(\spawner,
							Pspawner({ arg sp;
								sp.seq(Pdef(currentSynth.asSymbol));
								{ button.value = 0 }.defer;
						})).play(quant: 0);
					},{
						Pdef(currentSynth.asSymbol).stop;
						Pdef(\spawner).clear;
						Server.default.freeAll;
					}
				);
				// "playing...".postln; Pdef(currentSynth.asSymbol).play });

			})
			.front;

			// 'show me the code' button
			Button.new(
				parent: footer2,
				bounds: Rect.new(
					left: footer2.bounds.width / 9 * 7,
					top: 0,
					width: footer2.bounds.width / 9 * 2,
					height: 50
				)
			)
			.string_("show me the code")
			// .font_(Font(Font.default.name, 18))
			.action_({ arg button;
				// String.readNew(File.new(synthCode, "r"));
				if(currentSynth.notNil,
					{
						SCLOrkSynths.showMeTheCode(currentSynth);
					},
					{
						"Select a synth first!".postln;
					}
				);
				button.value.postln;

			})
			.front;

			window.onClose_({ window = nil });

			"opening SCLOrkSynths gui".postln;

		}, {
			"SCLOrkSynths GUI already open?".postln;
		});

	}


	*showMeTheCode { |synth|
		var sPath, pPath;
		var sString, pString, pStringLastAscii;
		sPath = synthDictionary[synth.asSymbol][\synthPath];
		pPath = synthDictionary[synth.asSymbol][\patternPath];
		sString = String.readNew(File.new(sPath, "r"));
		pString = String.readNew(File.new(pPath, "r"));

		while(
			{pStringLastAscii != 41}, // while this is *not* a ")" (ascii 41)
			// remove last characters until reaching the parenthesis:
			{
				pString = pString.drop(-1);
				pStringLastAscii = pString.last.ascii;
				// ["last ascii now is", lastAscii].postln;
			}
		);

		Document.new(
			title: synth.asString ++ " code",
			string:
			"// SynthDef\n(\n"
			++
			sString
			++
			"\n\n);\n// Pattern demo\n(\n"
			++
			pString
			++
			".play;\n);"
		).front;
	}


	*prOpenMoreDemos { |synth|
		var extraDemosPath;
		var pString, pStringLastAscii;
		extraDemosPath = SCLOrkSynths.folderPath +/+ "pbind-demos" +/+ synth.asString ++ "-demo-extras.scd";
		// Check if there is an demos-extra files for this SynthDef before proceeding
		if( PathName(extraDemosPath).isFile, {
			pString = String.readNew(File.new(extraDemosPath, "r"));

			while(
				{pStringLastAscii != 41}, // while this is *not* a ")" (ascii 41)
				// remove last characters until reaching the parenthesis:
				{
					pString = pString.drop(-1);
					pStringLastAscii = pString.last.ascii;
					// ["last ascii now is", lastAscii].postln;
				}
			);

			Document.new(
				title: synth.asString ++ " extra demos",
				string: pString
			).front;
		}, {
			"There are no additional demos for this SynthDef.".postln;
		});
	}

	// Prints directory of all synthDefs
	*directory {
		var list = synthDictionary.keys.asArray.sort;
		list.do({ |k| k.postln; });
		(list.size.asString ++ " Synth Definitions available").postln;
	}

} // end of SCLOrkSynths class definition
