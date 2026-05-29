/** Renders a Pokémon sprite from any PkmBaseDTO, reusing the upstream spritesheet primitive. */
import type React from 'react';
import { Gender } from '../../../data/sdk/model';
import type { PkmBaseDTO } from '../../../data/sdk/model';
import { SpeciesImg } from '../../../ui/img/species-img';

export const PkmSprite: React.FC<{ pkm: PkmBaseDTO; small?: boolean }> = ({ pkm, small }) => (
  <SpeciesImg
    species={pkm.species}
    context={pkm.context}
    form={pkm.form}
    isFemale={pkm.gender === Gender.Female}
    isShiny={pkm.isShiny}
    isEgg={pkm.isEgg}
    isShadow={pkm.isShadow}
    small={small}
  />
);
