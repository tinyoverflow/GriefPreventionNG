package me.tinyoverflow.griefprevention.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.tinyoverflow.griefprevention.data.repositories.ClaimRepository;
import me.tinyoverflow.griefprevention.data.repositories.UserRepository;

@Getter
public class RepositoryContainer
{

    private final UserRepository userRepository;
    private final ClaimRepository claimRepository;

    private RepositoryContainer(UserRepository userRepository, ClaimRepository claimRepository)
    {
        this.userRepository = userRepository;
        this.claimRepository = claimRepository;
    }

    public static RepositoryContainerBuilder builder()
    {
        return new RepositoryContainerBuilder();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RepositoryContainerBuilder
    {
        private UserRepository userRepository;
        private ClaimRepository claimRepository;

        public RepositoryContainerBuilder withUserRepository(UserRepository userRepository)
        {
            this.userRepository = userRepository;
            return this;
        }

        public RepositoryContainerBuilder withClaimRepository(ClaimRepository claimRepository)
        {
            this.claimRepository = claimRepository;
            return this;
        }

        public RepositoryContainer build()
        {
            return new RepositoryContainer(
                    userRepository,
                    claimRepository
            );
        }
    }
}
