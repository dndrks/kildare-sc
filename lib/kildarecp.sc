KildareCP {

	var <params;
	var <controlspecs;
	var <voiceGroup;

	*new { | out |
		^super.new.init(out)
	}

	init { | out |

		var s = Server.default;

		SynthDef(\kildare_cp, {
			arg out, stopGate = 1,
			carHz, carDetune,
			modHz, modAmp, modRel, feedAmp,
			modFollow, modNum, modDenum,
			carRel, amp, click,
			squishPitch, squishChunk,
			pan, amDepth, amHz,
			eqHz, eqAmp, bitRate, bitCount,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpDepth;

			var car, mod, carEnv, modEnv, carRamp, feedMod, feedCar, ampMod,
			mod_1, mod_2, filterEnv, mainSend;

			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);

			carHz = carHz * (2.pow(carDetune/12));
			modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);

			filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
			modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
			feedAmp = LinLin.kr(feedAmp,0.0,1.0,0.0,10.0);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);

			modEnv = EnvGen.ar(
				Env.new(
					[0, 1, 0, 0.9, 0, 0.7, 0, 0.5, 0],
					[0.001, 0.009, 0, 0.008, 0, 0.01, 0, modRel],
					curve: \lin
				),gate: stopGate
			);
			filterEnv = EnvGen.kr(Env.perc(lpAtk, lpRel, 1),gate: stopGate);
			carRamp = EnvGen.kr(Env([600, 0.000001], [0], curve: \lin));
			carEnv = EnvGen.ar(
				Env.new(
					[0, 1, 0, 0.9, 0, 0.7, 0, 0.5, 0],
					[0,0,0,0,0,0,0,carRel/4],
					[0, -3, 0, -3, 0, -3, 0, -3]
				),gate: stopGate
			);

			mod_2 = SinOscFB.ar(
				(modHz*4),
				feedAmp,
				0,
				modAmp*1
			)* modEnv;

			mod_1 = SinOscFB.ar(
				modHz+mod_2,
				feedAmp,
				modAmp*100
			)* modEnv;

			car = SinOsc.ar(carHz + (mod_1)) * carEnv * amp;
			car = RHPF.ar(in:car+(LPF.ar(Impulse.ar(0.003),12000,1)*click),freq:hpHz,rq:filterQ,mul:1);

			ampMod = SinOsc.ar(freq:amHz,mul:amDepth,add:1);
			car = car * ampMod;
			car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			car = Decimator.ar(car,bitRate,bitCount,1.0);
			car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
			car = RLPF.ar(in:car,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);

			car = car.softclip;
			mainSend = Pan2.ar(car,pan);
			mainSend = mainSend * amp;

			Out.ar(out, mainSend);

			FreeSelf.kr(Done.kr(modEnv) * Done.kr(carEnv));

		}).send;

		// build a list of our sound-shaping parameters, with default values
		// (see https://doc.sccode.org/Classes/Dictionary.html for more about Dictionaries):
		params = Dictionary.newFrom([
			\out,out,
			\poly,0,
			\amp,0.7,
			\carRel,0.43,
			\modAmp,1,
			\modHz,300,
			\modFollow,0,
			\modNum,1,
			\modDenum,1,
			\modRel,0.5,
			\feedAmp,1,
			\click,1,
			\squishPitch,1,
			\squishChunk,1,
			\amDepth,0,
			\amHz,2698.8,
			\eqHz,6000,
			\eqAmp,0,
			\bitRate,24000,
			\bitCount,24,
			\lpHz,24000,
			\hpHz,20,
			\filterQ,50,
			\lpAtk,0,
			\lpRel,0.3,
			\lpDepth,0,
			\pan,0,
		]);

		controlspecs = Dictionary.newFrom([
			\00, Dictionary.newFrom([\poly, ControlSpec.new(minval: 0.0, maxval: 1.0, warp: 'lin', step: 1, default: 0)]),
			\01, Dictionary.newFrom([\amp, ControlSpec.new(minval: 0.0, maxval: 1.0, warp: 'lin', step: 0.0, default: 0.5)]),
			\02, Dictionary.newFrom([\carRel, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.43)]),
			\03, Dictionary.newFrom([\modAmp, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 1)]),
			\04, Dictionary.newFrom([\modHz, ControlSpec.new(minval: 20, maxval: 24000, warp: 'exp', units: 'hz', default: 300)]),
			\05, Dictionary.newFrom([\modFollow, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', step: 1, default: 0)]),
			\06, Dictionary.newFrom([\modNum, ControlSpec.new(minval: -20, maxval: 20, warp: 'lin', default: 1)]),
			\07, Dictionary.newFrom([\modDenum, ControlSpec.new(minval: -20, maxval: 20, warp: 'lin', default: 1)]),
			\08, Dictionary.newFrom([\modRel, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.5)]),
			\09, Dictionary.newFrom([\feedAmp, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 1)]),
			\10, Dictionary.newFrom([\click, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\11, Dictionary.newFrom([\squishPitch, ControlSpec.new(minval: 1, maxval: 10, warp: 'lin', default: 1)]),
			\12, Dictionary.newFrom([\squishChunk, ControlSpec.new(minval: 1, maxval: 10, warp: 'lin', default: 1)]),
			\13, Dictionary.newFrom([\amDepth, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\14, Dictionary.newFrom([\amHz, ControlSpec.new(minval: 0.001, maxval: 12000, warp: 'exp', units: 'hz', default: 2698.8)]),
			\15, Dictionary.newFrom([\eqHz, ControlSpec.new(minval: 20, maxval: 20000, warp: 'exp', units: 'hz', default: 6000)]),
			\16, Dictionary.newFrom([\eqAmp, ControlSpec.new(minval: -2, maxval: 2, warp: 'lin', default: 0)]),
			\17, Dictionary.newFrom([\bitRate, ControlSpec.new(minval: 20, maxval: 24000, warp: 'exp', units: 'hz', default: 24000)]),
			\18, Dictionary.newFrom([\bitCount, ControlSpec.new(minval: 1, maxval: 24, warp: 'lin', units: 'bits', default: 24)]),
			\19, Dictionary.newFrom([\lpHz, ControlSpec.new(minval: 20, maxval: 20000, warp: 'exp', units: 'hz', default: 20000)]),
			\20, Dictionary.newFrom([\lpAtk, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.001)]),
			\21, Dictionary.newFrom([\lpRel, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.3)]),
			\22, Dictionary.newFrom([\lpDepth, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\23, Dictionary.newFrom([\hpHz, ControlSpec.new(minval: 20, maxval: 24000, warp: 'exp', units: 'hz', default: 20)]),
			\24, Dictionary.newFrom([\filterQ, ControlSpec.new(minval: 0, maxval: 100, warp: 'lin', units: '%', default: 50)]),
			\25, Dictionary.newFrom([\pan, ControlSpec.new(minval: -1, maxval: 1, warp: 'lin', default: 0)]),
		]);

	// NEW: register 'voiceGroup' as a Group on the Server
		voiceGroup = Group.new(s);
	}


	trigger {
		if( params[\poly] == 0,{
			voiceGroup.set(\stopGate, -1.1);
			Synth.new(\kildare_cp, params.getPairs, voiceGroup);
		},{
		// NEW: set the target of every Synth voice to the 'voiceGroup' Group
		Synth.new(\kildare_cp, params.getPairs, voiceGroup);
		});
	}

	setParam { arg paramKey, paramValue;
		// NEW: send changes to the paramKey, paramValue pair immediately to all voices
		if( params[\poly] == 0,{
			voiceGroup.set(paramKey, paramValue);
		});
		params[paramKey] = paramValue;
	}

	// NEW: free our Group when the class is freed
	free {
		voiceGroup.free;
	}

}