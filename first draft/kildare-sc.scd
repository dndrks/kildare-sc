(

Window.closeAll;

Routine{
	Server.default.options.outDevice_("BlackHole 16ch");

	s.options.numOutputBusChannels = 16;

	s.reboot;

	2.wait;

	~voices = [
		~bd = KildareBD.new(0),
		~sd = KildareSD.new(2),
		~tm = KildareTM.new(4),
		~cp = KildareCP.new(6),
		~rs = KildareRS.new(8),
		~cb = KildareCB.new(10),
		~hh = KildareHH.new(12),
	];

	~ui_render = true; // change <false> to <true> if not using Max/M4L interfaces

	~voice_strings = Dictionary.newFrom([
		\bd, 0,
		\sd, 1,
		\tm, 2,
		\cp, 3,
		\rs, 4,
		\cb, 5,
		\hh, 6,
	]);

	~voice_windows = Dictionary.newFrom([
		~bd, [\bd, 0, 600, 28],
		~sd, [\sd, 305, 600, 31],
		~tm, [\tm, 610, 600, 28],
		~cp, [\cp, 915, 600, 25],
		~rs, [\rs, 1220, 600, 28],
		~cb, [\cb, 0, 0, 22.2],
		~hh, [\hh, 305, 0, 28],
	]);

	MIDIClient.init;
	MIDIClient.sources;
	MIDIIn.connect(0,MIDIClient.sources[0]);

	~incomingMIDI = MIDIFunc.noteOn({ |velocity, midipitch, channel|

		if( channel <= 7,{
			~voices[channel].setParam(\carHz, midipitch.midicps);
			~voices[channel].setParam(\amp, LinLin.kr(velocity,0,127,0.0,1.0));
			~voices[channel].trigger();
		});

	});

	~param_OSC = OSCFunc({
		arg msg;
		~voices[~voice_strings[msg[1]]].setParam(msg[2], msg[3]);
	}, '/kildare_param');

	~trig_OSC = OSCFunc({
		arg msg;
		~voices[~voice_strings[msg[1]]].trigger();
	}, '/kildare_trig');


	if( ~ui_render, {
		~voices.do({ arg drum;
			{
				w=Window.new(
					name: ~voice_windows[drum][0],
					bounds: Rect(~voice_windows[drum][1],~voice_windows[drum][2],300,19*~voice_windows[drum][3])
				).front;
				w.bounds.postln;
				w.view.decorator=FlowLayout(w.view.bounds);
				w.view.decorator.gap=2@2;

				~controls = drum.controlspecs.keys;
				~controls = ~controls.asArray;
				~controls = ~controls.sort;

				~controls.do({ arg id;
					var name = drum.controlspecs[id].asKeyValuePairs[0],
					spec = drum.controlspecs[id].asKeyValuePairs[1];
					EZSlider(w, 300@16, name, spec,
						action: { arg slider;
							drum.setParam(name, slider.value);
						},
						labelWidth: 70, unitWidth:30, numberWidth:60, layout:\horz)
					.setColors(Color.grey,Color.white)
					.font_(Font("Helvetica",11));
				});
			}.defer;
		});
	});

}.play;

~param_OSC.free;
~trig_OSC.free;
~incomingMIDI.free;

)

~bd.params['lpAtk']