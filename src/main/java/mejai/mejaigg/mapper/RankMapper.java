package mejai.mejaigg.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import mejai.mejaigg.domain.Rank;
import mejai.mejaigg.dto.riot.RankDto;

@Mapper
public interface RankMapper {
	RankMapper INSTANCE = Mappers.getMapper(RankMapper.class);

	@Mapping(source = "rankDto.rank", target = "rank")
	Rank toRankEntity(RankDto rankDto);

}
