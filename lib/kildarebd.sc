KildareBD {

	var <params;
	var <controlspecs;
	var <voiceGroup;

	*new { | out |
		^super.new.init(out)
	}

	init { | out |

		var s = Server.default;

		SynthDef(\kildare_bd, {
			arg out, stopGate = 1,
			amp, carHz, carDetune, carAtk, carRel,
			modHz, modAmp, modAtk, modRel, feedAmp,
			modFollow, modNum, modDenum,
			pan, rampDepth, rampDec,
			squishPitch, squishChunk,
			amDepth, amHz,
			eqHz, eqAmp, bitRate, bitCount,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpDepth;

			var car, mod, carEnv, modEnv, carRamp,
			feedMod, feedCar, ampMod, click, clicksound,
			mod_1, filterEnv, mainSend;

			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);
			modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);

			filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
			modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
			feedAmp = LinLin.kr(feedAmp,0.0,1.0,0.0,10.0);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
			amDepth = LinLin.kr(amDepth,0.0,1.0,0.0,2.0);
			carHz = carHz * (2.pow(carDetune/12));

			modEnv = EnvGen.kr(Env.perc(modAtk, modRel),gate: stopGate);
			filterEnv = EnvGen.kr(Env.perc(lpAtk, lpRel, 1),gate: stopGate);
			carRamp = EnvGen.kr(Env([1000, 0.000001], [rampDec], curve: \exp));
			carEnv = EnvGen.kr(Env.perc(carAtk, carRel),gate: stopGate, doneAction:2);

			mod_1 = SinOscFB.ar(
				modHz+ ((carRamp*3)*rampDepth),
				feedAmp,
				modAmp*10
			)* modEnv;

			car = SinOsc.ar(carHz + (mod_1) + (carRamp*rampDepth)) * carEnv;

			ampMod = SinOsc.ar(freq:amHz,mul:(amDepth/2),add:1);
			click = amp/4;
			clicksound = LPF.ar(Impulse.ar(0.003),16000,click) * EnvGen.kr(envelope: Env.perc(carAtk, 0.2), gate: stopGate);
			car = (car + clicksound)* ampMod;

			car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			car = Decimator.ar(car,bitRate,bitCount,1.0);
			car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
			car = RLPF.ar(in:car,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);
			car = Compander.ar(in:car, control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);

			mainSend = Pan2.ar(car,pan);
			mainSend = mainSend * amp;

			Out.ar(out, mainSend);
		}).send;

		// build a list of our sound-shaping parameters, with default values
		// (see https://doc.sccode.org/Classes/Dictionary.html for more about Dictionaries):
		params = Dictionary.newFrom([
			\out,out,
			\poly,0,
			\amp,0.7,
			\carHz,55,
			\carDetune,0,
			\carAtk,0,
			\carRel,0.3,
			\modAmp,0,
			\modHz,600,
			\modFollow,0,
			\modNum,1,
			\modDenum,1,
			\modAtk,0,
			\modRel,0.05,
			\feedAmp,1,
			\rampDepth,0.11,
			\rampDec,0.3,
			\squishPitch,1,
			\squishChunk,1,
			\amDepth,0,
			\amHz,8175.08,
			\eqHz,6000,
			\eqAmp,0,
			\bitRate,24000,
			\bitCount,24,
			\lpHz,19000,
			\hpHz,0,
			\filterQ,50,
			\lpAtk,0,
			\lpRel,0.3,
			\lpDepth,0,
			\pan,0,
		]);

		controlspecs = Dictionary.newFrom([
			\00, Dictionary.newFrom([\poly, ControlSpec.new(minval: 0.0, maxval: 1.0, warp: 'lin', step: 1, default: 0)]),
			\01, Dictionary.newFrom([\amp, ControlSpec.new(minval: 0.0, maxval: 1.0, warp: 'lin', step: 0.0, default: 0.7)]),
			\02, Dictionary.newFrom([\carAtk, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0)]),
			\03, Dictionary.newFrom([\carRel, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.3)]),
			\04, Dictionary.newFrom([\modAmp, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\05, Dictionary.newFrom([\modHz, ControlSpec.new(minval: 20, maxval: 24000, warp: 'exp', units: 'hz', default: 600)]),
			\06, Dictionary.newFrom([\modFollow, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', step: 1, default: 0)]),
			\07, Dictionary.newFrom([\modNum, ControlSpec.new(minval: -20, maxval: 20, warp: 'lin', default: 1)]),
			\08, Dictionary.newFrom([\modDenum, ControlSpec.new(minval: -20, maxval: 20, warp: 'lin', default: 1)]),
			\09, Dictionary.newFrom([\modAtk, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.001)]),
			\10, Dictionary.newFrom([\modRel, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.05)]),
			\11, Dictionary.newFrom([\feedAmp, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 1)]),
			\12, Dictionary.newFrom([\rampDepth, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0.11)]),
			\13, Dictionary.newFrom([\rampDec, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.3)]),
			\14, Dictionary.newFrom([\squishPitch, ControlSpec.new(minval: 1, maxval: 10, warp: 'lin', default: 1)]),
			\15, Dictionary.newFrom([\squishChunk, ControlSpec.new(minval: 1, maxval: 10, warp: 'lin', default: 1)]),
			\16, Dictionary.newFrom([\amDepth, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\17, Dictionary.newFrom([\amHz, ControlSpec.new(minval: 0.001, maxval: 12000, warp: 'exp', default: 8175.08)]),
			\18, Dictionary.newFrom([\eqHz, ControlSpec.new(minval: 20, maxval: 20000, warp: 'exp', units: 'hz', default: 6000)]),
			\19, Dictionary.newFrom([\eqAmp, ControlSpec.new(minval: -2, maxval: 2, warp: 'lin', default: 0)]),
			\20, Dictionary.newFrom([\bitRate, ControlSpec.new(minval: 20, maxval: 24000, warp: 'exp', units: 'hz', default: 24000)]),
			\21, Dictionary.newFrom([\bitCount, ControlSpec.new(minval: 1, maxval: 24, warp: 'lin', units: 'bits', default: 24)]),
			\22, Dictionary.newFrom([\lpHz, ControlSpec.new(minval: 20, maxval: 20000, warp: 'exp', units: 'hz', default: 20000)]),
			\23, Dictionary.newFrom([\lpAtk, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.001)]),
			\24, Dictionary.newFrom([\lpRel, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.05)]),
			\25, Dictionary.newFrom([\lpDepth, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\26, Dictionary.newFrom([\hpHz, ControlSpec.new(minval: 20, maxval: 24000, warp: 'exp', units: 'hz', default: 20)]),
			\27, Dictionary.newFrom([\filterQ, ControlSpec.new(minval: 0, maxval: 100, warp: 'lin', units: '%', default: 50)]),
			\28, Dictionary.newFrom([\pan, ControlSpec.new(minval: -1, maxval: 1, warp: 'lin', default: 0)]),
		]);

	// NEW: register 'voiceGroup' as a Group on the Server
		voiceGroup = Group.new(s);
	}


	trigger {
		if( params[\poly] == 0,{
			voiceGroup.set(\stopGate, -1.1);
			Synth.new(\kildare_bd, params.getPairs, voiceGroup);
		},{
		// NEW: set the target of every Synth voice to the 'voiceGroup' Group
		Synth.new(\kildare_bd, params.getPairs, voiceGroup);
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