KildareRS {

	var <params;
	var <controlspecs;
	var <voiceGroup;

	*new { | out |
		^super.new.init(out)
	}

	init { | out |

		var s = Server.default;

		SynthDef(\kildare_rs, {
			arg out, stopGate = 1,
			carHz, carDetune,
			modHz, modAmp,
			modFollow, modNum, modDenum,
			carAtk, carRel, amp,
			pan, rampDepth, rampDec, amDepth, amHz,
			eqHz, eqAmp, bitRate, bitCount,
			sdAmp, sdRel, sdAtk,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpDepth,
			squishPitch, squishChunk;

			var car, mod, carEnv, modEnv, carRamp, feedMod, feedCar, ampMod,
			mod_1,mod_2,feedAmp,feedAMP, sd_modHz,
			sd_car, sd_mod, sd_carEnv, sd_modEnv, sd_carRamp, sd_feedMod, sd_feedCar, sd_noise, sd_noiseEnv,
			sd_mix, filterEnv, mainSendCar, mainSendSnare;

			amp = amp*0.35;
			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);

			carHz = carHz * (2.pow(carDetune/12));
			modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);

			filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
			modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
			amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);

			feedAmp = modAmp.linlin(0, 127, 0, 3);
			feedAMP = modAmp.linlin(0, 127, 0, 4);

			carRamp = EnvGen.kr(Env([600, 0.000001], [rampDec], curve: \lin));
			carEnv = EnvGen.kr(Env.perc(carAtk, carRel),gate: stopGate);
			filterEnv = EnvGen.kr(Env.perc(lpAtk, lpRel, 1),gate: stopGate);

			mod_2 = SinOscFB.ar(
				modHz*16,
				feedAmp,
				modAmp*10
			)* 1;

			mod_1 = SinOscFB.ar(
				modHz+mod_2,
				feedAmp,
				modAmp*10
			)* 1;

			car = SinOscFB.ar(carHz + (mod_1+mod_2) + (carRamp*rampDepth),feedAMP) * carEnv * amp;

			ampMod = SinOsc.ar(freq:amHz,mul:amDepth,add:1);
			car = (car+(LPF.ar(Impulse.ar(0.003),16000,1)*amp)) * ampMod;
			car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			car = Decimator.ar(car,bitRate,bitCount,1.0);
			car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
			car = RLPF.ar(in:car,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);
			car = LPF.ar(car,12000,1);

			sd_modHz = carHz*2.52;
			sd_modEnv = EnvGen.kr(Env.perc(carAtk, carRel));
			sd_carRamp = EnvGen.kr(Env([1000, 0.000001], [rampDec], curve: \exp));
			sd_carEnv = EnvGen.kr(Env.perc(sdAtk, sdRel),gate:stopGate);
			sd_feedMod = SinOsc.ar(modHz, mul:modAmp*100) * sd_modEnv;
			sd_feedCar = SinOsc.ar(carHz + sd_feedMod + (carRamp*rampDepth)) * sd_carEnv * (feedAmp*10);
			sd_mod = SinOsc.ar(modHz + sd_feedCar, mul:modAmp) * sd_modEnv;
			sd_car = SinOsc.ar(carHz + sd_mod + (carRamp*rampDepth)) * sd_carEnv * sdAmp;
			sd_car = sd_car * ampMod;
			sd_mix = Squiz.ar(in:sd_car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			sd_mix = Decimator.ar(sd_mix,bitRate,bitCount,1.0);
			sd_mix = BPeakEQ.ar(in:sd_mix,freq:eqHz,rq:1,db:eqAmp,mul:1);
			sd_mix = RLPF.ar(in:sd_mix,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			sd_mix = RHPF.ar(in:sd_mix,freq:hpHz, rq: filterQ, mul:1);

			car = car.softclip;
			car = Compander.ar(in:car,control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			mainSendCar = Pan2.ar(car,pan);
			mainSendCar = mainSendCar * amp;

			sd_mix = sd_mix.softclip;
			sd_mix = Compander.ar(in:sd_mix,control:sd_mix, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			mainSendSnare = Pan2.ar(sd_mix,pan);
			mainSendSnare = mainSendSnare * amp;

			Out.ar(out, mainSendCar);
			Out.ar(out, mainSendSnare);

			FreeSelf.kr(Done.kr(sd_carEnv) * Done.kr(carEnv));

		}).send;

		// build a list of our sound-shaping parameters, with default values
		// (see https://doc.sccode.org/Classes/Dictionary.html for more about Dictionaries):
		params = Dictionary.newFrom([
			\out,out,
			\poly,0,
			\amp,0.7,
			\carAtk,0,
			\carRel,0.05,
			\modAmp,1,
			\modHz,4000,
			\modFollow,0,
			\modNum,1,
			\modDenum,1,
			\sdAmp,1,
			\sdAtk,0,
			\sdRel,0.05,
			\rampDepth,0,
			\rampDec,0.06,
			\squishPitch,1,
			\squishChunk,1,
			\amDepth,0,
			\amHz,8175.08,
			\eqHz,6000,
			\eqAmp,0,
			\bitRate,24000,
			\bitCount,24,
			\lpHz,19000,
			\hpHz,20,
			\filterQ,50,
			\lpAtk,0,
			\lpRel,0.3,
			\lpDepth,0,
			\pan,0,
		]);

		controlspecs = Dictionary.newFrom([
			\00, Dictionary.newFrom([\poly, ControlSpec.new(minval: 0.0, maxval: 1.0, warp: 'lin', step: 1, default: 0)]),
			\01, Dictionary.newFrom([\amp, ControlSpec.new(minval: 0.0, maxval: 1.0, warp: 'lin', step: 0.0, default: 0.7)]),
			\03, Dictionary.newFrom([\carAtk, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0)]),
			\04, Dictionary.newFrom([\carRel, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.05)]),
			\05, Dictionary.newFrom([\modAmp, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 1)]),
			\06, Dictionary.newFrom([\modHz, ControlSpec.new(minval: 20, maxval: 24000, warp: 'exp', units: 'hz', default: 4000)]),
			\07, Dictionary.newFrom([\modFollow, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', step: 1, default: 0)]),
			\08, Dictionary.newFrom([\modNum, ControlSpec.new(minval: -20, maxval: 20, warp: 'lin', default: 1)]),
			\09, Dictionary.newFrom([\modDenum, ControlSpec.new(minval: -20, maxval: 20, warp: 'lin', default: 1)]),
			\10, Dictionary.newFrom([\sdAmp, ControlSpec.new(minval: 0.0, maxval: 1.0, warp: 'lin', step: 0.0, default: 1)]),
			\11, Dictionary.newFrom([\sdAtk, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0)]),
			\12, Dictionary.newFrom([\sdRel, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.05)]),
			\13, Dictionary.newFrom([\rampDepth, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\14, Dictionary.newFrom([\rampDec, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.06)]),
			\15, Dictionary.newFrom([\squishPitch, ControlSpec.new(minval: 1, maxval: 10, warp: 'lin', default: 1)]),
			\16, Dictionary.newFrom([\squishChunk, ControlSpec.new(minval: 1, maxval: 10, warp: 'lin', default: 1)]),
			\17, Dictionary.newFrom([\amDepth, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\18, Dictionary.newFrom([\amHz, ControlSpec.new(minval: 0.001, maxval: 12000, warp: 'exp', units: 'hz', default: 2698.8)]),
			\19, Dictionary.newFrom([\eqHz, ControlSpec.new(minval: 20, maxval: 20000, warp: 'exp', units: 'hz', default: 6000)]),
			\20, Dictionary.newFrom([\eqAmp, ControlSpec.new(minval: -2, maxval: 2, warp: 'lin', default: 0)]),
			\21, Dictionary.newFrom([\bitRate, ControlSpec.new(minval: 20, maxval: 24000, warp: 'exp', units: 'hz', default: 24000)]),
			\22, Dictionary.newFrom([\bitCount, ControlSpec.new(minval: 1, maxval: 24, warp: 'lin', units: 'bits', default: 24)]),
			\23, Dictionary.newFrom([\lpHz, ControlSpec.new(minval: 20, maxval: 20000, warp: 'exp', units: 'hz', default: 20000)]),
			\24, Dictionary.newFrom([\lpAtk, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.001)]),
			\25, Dictionary.newFrom([\lpRel, ControlSpec.new(minval: 0.001, maxval: 10, warp: 'exp', units: 's', default: 0.5)]),
			\26, Dictionary.newFrom([\lpDepth, ControlSpec.new(minval: 0, maxval: 1, warp: 'lin', default: 0)]),
			\27, Dictionary.newFrom([\hpHz, ControlSpec.new(minval: 20, maxval: 24000, warp: 'exp', units: 'hz', default: 20)]),
			\28, Dictionary.newFrom([\filterQ, ControlSpec.new(minval: 0, maxval: 100, warp: 'lin', units: '%', default: 50)]),
			\29, Dictionary.newFrom([\pan, ControlSpec.new(minval: -1, maxval: 1, warp: 'lin', default: 0)]),
		]);

	// NEW: register 'voiceGroup' as a Group on the Server
		voiceGroup = Group.new(s);
	}


	trigger {
		if( params[\poly] == 0,{
			voiceGroup.set(\stopGate, -1.1);
			Synth.new(\kildare_rs, params.getPairs, voiceGroup);
		},{
		// NEW: set the target of every Synth voice to the 'voiceGroup' Group
		Synth.new(\kildare_rs, params.getPairs, voiceGroup);
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