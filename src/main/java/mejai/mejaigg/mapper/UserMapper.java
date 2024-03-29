package mejai.mejaigg.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import mejai.mejaigg.domain.User;
import mejai.mejaigg.dto.riot.AccountDto;
import mejai.mejaigg.dto.riot.SummonerDto;

@Mapper
public interface UserMapper {
	UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

	@Mappings({
		// Assuming we take puuid from AccountDto; it could be from SummonerDto as well
		@Mapping(source = "accountDto.puuid", target = "puuid"),
		@Mapping(source = "summonerDto.id", target = "summonerId"),
		@Mapping(source = "accountDto.gameName", target = "summonerName"),
		// Ignoring the collections as before
		@Mapping(target = "rank", ignore = true),
		@Mapping(target = "matchParticipants", ignore = true)
	})
	User toUserEntity(AccountDto accountDto, SummonerDto summonerDto);
}
