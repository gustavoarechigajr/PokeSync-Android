import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BackendErrorsContext } from './data/backend-errors-context.tsx';
import { DataProvider } from "./data/data-provider.tsx";
import { SplashMain } from './splash/splash-main.tsx';
import { PokeSyncApp } from './pokesync/app/PokeSyncApp.tsx';

// New PokeSync UI. The providers below load settings + static data; PokeSyncApp is the new presentation
// layer (see src/pokesync/README.md). The legacy PKVault router UI is retained in-repo but unmounted.
createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BackendErrorsContext.Provider>
      <DataProvider>
        <SplashMain>
          <PokeSyncApp />
        </SplashMain>
      </DataProvider>
    </BackendErrorsContext.Provider>
  </StrictMode>
);
