KildareSD {

	var <params;
	var <controlspecs;
	var <voiceGroup;

	*new { | out |
		^super.new.init(out)
	}

	init { | out |

		var s = Server.default;

		SynthDef(\kildare_sd, {
			arg out, stopGate = 1,
			carHz, carDetune, carAtk, carRel,
			modHz, modAmp, modAtk, modRel, feedAmp,
			modFollow, modNum, modDenum,
			amp, pan,
			rampDepth, rampDec, noiseAmp,
			noiseAtk, noiseRel, bitRate, bitCount,
			eqHz,eqAmp,
			squishPitch, squishChunk,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpDepth,
			amDepth, amHz;

			var car, mod, carEnv, modEnv, carRamp, feedMod, feedCar,
			noise, noiseEnv, mix, ampMod, filterEnv, mainSendCar, mainSendNoise;

			amp = amp;
			noiseAmp = noiseAmp/2;
			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);

			carHz = carHz * (2.pow(carDetune/12));
			modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);

			filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
			modEnv = EnvGen.kr(Env.perc(modAtk, modRel));
			filterEnv = EnvGen.kr(Env.perc(lpAtk, lpRel, 1),gate: stopGate);
			carRamp = EnvGen.kr(Env([1000, 0.000001], [rampDec], curve: \exp));
			carEnv = EnvGen.kr(Env.perc(carAtk, carRel),gate: stopGate);
			modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
			feedMod = SinOsc.ar(modHz, mul:modAmp*100) * modEnv;
			feedAmp = LinLin.kr(feedAmp,0,1,0.0,10.0);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			feedAmp = feedAmp * modAmp;
			rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
			amDepth = LinLin.kr(amDepth,0.0,1.0,0.0,2.0);

			feedCar = SinOsc.ar(carHz + feedMod + (carRamp*rampDepth)) * carEnv * (feedAmp/modAmp * 127);
			mod = SinOsc.ar(modHz + feedCar, mul:modAmp*100) * modEnv;
			car = SinOsc.ar(carHz + mod + (carRamp*rampDepth)) * carEnv;

			noiseEnv = EnvGen.kr(Env.perc(noiseAtk,noiseRel),gate: stopGate);
			noise = BPF.ar(WhiteNoise.ar(0.24),8000,1.3) * (noiseAmp*noiseEnv);
			noise = BPeakEQ.ar(in:noise,freq:eqHz,rq:1,db:eqAmp,mul:1);
			noise = RLPF.ar(in:noise, freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			noise = RHPF.ar(in:noise,freq:hpHz, rq: filterQ, mul:1);

			ampMod = SinOsc.ar(freq:amHz,mul:(amDepth/2),add:1);
			car = car * ampMod;
			car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			noise = Squiz.ar(in:noise, pitchratio:squishPitch, zcperchunk:squishChunk*100, mul:1);
			car = Decimator.ar(car,bitRate,bitCount,1.0);
			car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
			car = RLPF.ar(in:car,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);

			car = Compander.ar(in:car, control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			mainSendCar = Pan2.ar(car,pan);
			mainSendCar = mainSendCar * amp;

			noise = Compander.ar(in:noise, control:noise, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			mainSendNoise = Pan2.ar(noise,pan);
			mainSendNoise = mainSendNoise * amp;

			Out.ar(out, mainSendCar);
			Out.ar(out, mainSendNoise);

			FreeSelf.kr(Done.kr(carEnv) * Done.kr(noiseEnv));
		}).send;

		// build a list of our sound-shaping parameters, with default values
		// (see https://doc.sccode.org/Classes/Dictionary.html for more about Dictionaries):
		params = Dictionary.newFrom([
			\out,out,
			\poly,0,
			\amp,0.7,
			\carHz,282.54,
			\carDetune,0,
			\carAtk,0,
			\carRel,0.15,
			\modAmp,0,
			\modHz,2770,
			\modFollow,0,
			\modNum,1,
			\modDenum,1,
			\modAtk,0.2,
			\modRel,1,
			\feedAmp,0,
			\noiseAmp,0.01,
			\noiseAtk,0,
			\noiseRel,0.1,
			\rampDepth,0.5,
			\rampDec,0.06,
			\squishPitch,1,
			\squishChunk,1,
			\amDepth,0,
			\amHz,2698.8,
			\eqHz,12000,
			\eqAmp,1,
			\bitRate,24000,
			\bitCount,24,
			\lpHz,24000,
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
			\12, Dictionary.newFrom([\noiseAmp, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0.01)]),
			\13, Dictionary.newFrom([\noiseAtk, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.001)]),
			\14, Dictionary.newFrom([\noiseRel, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.01)]),
			\15, Dictionary.newFrom([\rampDepth, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0.11)]),
			\16, Dictionary.newFrom([\rampDec, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.3)]),
			\17, Dictionary.newFrom([\squishPitch, ControlSpec.new(minval: 1, maxval: 10, warp: 'lin', default: 1)]),
			\18, Dictionary.newFrom([\squishChunk, ControlSpec.new(minval: 1, maxval: 10, warp: 'lin', default: 1)]),
			\19, Dictionary.newFrom([\amDepth, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\20, Dictionary.newFrom([\amHz, ControlSpec.new(minval: 0.001, maxval: 12000, warp: 'exp', units: 'hz', default: 8175.08)]),
			\21, Dictionary.newFrom([\eqHz, ControlSpec.new(minval: 20, maxval: 20000, warp: 'exp', units: 'hz', default: 6000)]),
			\22, Dictionary.newFrom([\eqAmp, ControlSpec.new(minval: -2, maxval: 2, warp: 'lin', default: 0)]),
			\23, Dictionary.newFrom([\bitRate, ControlSpec.new(minval: 20, maxval: 24000, warp: 'exp', units: 'hz', default: 24000)]),
			\24, Dictionary.newFrom([\bitCount, ControlSpec.new(minval: 1, maxval: 24, warp: 'lin', units: 'bits', default: 24)]),
			\25, Dictionary.newFrom([\lpHz, ControlSpec.new(minval: 20, maxval: 20000, warp: 'exp', units: 'hz', default: 20000)]),
			\26, Dictionary.newFrom([\lpAtk, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.001)]),
			\27, Dictionary.newFrom([\lpRel, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.05)]),
			\28, Dictionary.newFrom([\lpDepth, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\29, Dictionary.newFrom([\hpHz, ControlSpec.new(minval: 20, maxval: 24000, warp: 'exp', units: 'hz', default: 20)]),
			\30, Dictionary.newFrom([\filterQ, ControlSpec.new(minval: 0, maxval: 100, warp: 'lin', units: '%', default: 50)]),
			\31, Dictionary.newFrom([\pan, ControlSpec.new(minval: -1, maxval: 1, warp: 'lin', default: 0)]),
		]);

	// NEW: register 'voiceGroup' as a Group on the Server
		voiceGroup = Group.new(s);
	}


	trigger {
		if( params[\poly] == 0,{
			voiceGroup.set(\stopGate, -1.1);
			Synth.new(\kildare_sd, params.getPairs, voiceGroup);
		},{
		// NEW: set the target of every Synth voice to the 'voiceGroup' Group
		Synth.new(\kildare_sd, params.getPairs, voiceGroup);
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