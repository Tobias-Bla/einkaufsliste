const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const openAiApiKey = defineSecret("OPENAI_API_KEY");

const recommendationSchema = {
  type: "object",
  additionalProperties: false,
  required: ["recommendations"],
  properties: {
    recommendations: {
      type: "array",
      items: {
        type: "object",
        additionalProperties: false,
        required: [
          "id",
          "recipeId",
          "name",
          "description",
          "score",
          "matchedIngredients",
          "missingIngredients",
          "offerMatches",
          "rationale",
          "imageUrl"
        ],
        properties: {
          id: { type: "string" },
          recipeId: { type: "string" },
          name: { type: "string" },
          description: { type: "string" },
          score: { type: "integer" },
          matchedIngredients: {
            type: "array",
            items: { type: "string" }
          },
          missingIngredients: {
            type: "array",
            items: {
              type: "object",
              additionalProperties: false,
              required: ["name", "amount", "unit", "category", "isCore"],
              properties: {
                name: { type: "string" },
                amount: { type: "string" },
                unit: { type: "string" },
                category: { type: "string" },
                isCore: { type: "boolean" }
              }
            }
          },
          offerMatches: {
            type: "array",
            items: {
              type: "object",
              additionalProperties: false,
              required: [
                "offerId",
                "storeName",
                "offerTitle",
                "ingredientName",
                "priceLabel"
              ],
              properties: {
                offerId: { type: "string" },
                storeName: { type: "string" },
                offerTitle: { type: "string" },
                ingredientName: { type: "string" },
                priceLabel: { type: "string" }
              }
            }
          },
          rationale: { type: "string" },
          imageUrl: { type: "string" }
        }
      }
    }
  }
};

exports.generateRecipeRecommendations = onCall(
  {
    secrets: [openAiApiKey],
    timeoutSeconds: 30,
    memory: "1GiB"
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Authentication required.");
    }

    const profile = request.data?.profile;
    const offers = Array.isArray(request.data?.offers) ? request.data.offers : [];
    const savedRecipes = Array.isArray(request.data?.savedRecipes) ? request.data.savedRecipes : [];

    if (!profile || offers.length === 0) {
      throw new HttpsError("invalid-argument", "Missing offer profile or offers.");
    }

    const apiKey = openAiApiKey.value();
    const prompt = buildPrompt({ profile, offers, savedRecipes });
    const openAiResponse = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: "gpt-4o-mini",
        input: [
          {
            role: "system",
            content: [
              {
                type: "input_text",
                text:
                  "You create practical German grocery recipe recommendations. " +
                  "Return only JSON matching the schema. Prefer saved recipes when offer fit is strong, " +
                  "but propose a few new ideas when the offers justify it. Keep names and descriptions short."
              }
            ]
          },
          {
            role: "user",
            content: [
              {
                type: "input_text",
                text: prompt
              }
            ]
          }
        ],
        text: {
          format: {
            type: "json_schema",
            name: "recipe_recommendations",
            strict: true,
            schema: recommendationSchema
          }
        }
      })
    });

    if (!openAiResponse.ok) {
      const errorText = await openAiResponse.text();
      logger.error("OpenAI request failed", {
        status: openAiResponse.status,
        body: errorText
      });
      throw new HttpsError("internal", "OpenAI request failed.");
    }

    const payload = await openAiResponse.json();
    const rawText = extractOutputText(payload);
    if (!rawText) {
      logger.error("OpenAI response did not include output text", payload);
      throw new HttpsError("internal", "OpenAI returned no structured content.");
    }

    try {
      const parsed = JSON.parse(rawText);
      return sanitizeRecommendations(parsed, savedRecipes);
    } catch (error) {
      logger.error("Could not parse OpenAI response JSON", { error, rawText });
      throw new HttpsError("internal", "OpenAI response could not be parsed.");
    }
  }
);

function buildPrompt({ profile, offers, savedRecipes }) {
  const lines = [];
  lines.push(`City: ${profile.city}`);
  lines.push(`Radius: ${profile.radiusKm} km`);
  lines.push(`Preferred chains: ${(profile.preferredChains || []).join(", ") || "unknown"}`);
  lines.push("");
  lines.push("Current offers:");
  offers.forEach((offer) => {
    lines.push(
      `- ${offer.storeName} (${offer.chain}, ${offer.distanceKm} km): ${offer.title} | ` +
        `${offer.priceLabel} | tags: ${(offer.tags || []).join(", ")}`
    );
  });
  lines.push("");
  lines.push("Saved recipes:");
  if (savedRecipes.length === 0) {
    lines.push("- none");
  } else {
    savedRecipes.forEach((recipe) => {
      const ingredients = Array.isArray(recipe.ingredients)
        ? recipe.ingredients
            .map((ingredient) => {
              const amount = [ingredient.amount, ingredient.unit].filter(Boolean).join(" ").trim();
              return `${ingredient.name}${amount ? ` (${amount})` : ""}`;
            })
            .join(", ")
        : "";
      lines.push(`- [${recipe.id}] ${recipe.name}: ${ingredients}`);
    });
  }
  lines.push("");
  lines.push("Return 3 to 5 recommendations.");
  lines.push("Use recipeId only when clearly referring to one saved recipe from the list above, otherwise use an empty string.");
  lines.push("For new ideas, leave imageUrl empty.");
  lines.push("Descriptions and rationale should be in German.");
  return lines.join("\n");
}

function extractOutputText(payload) {
  if (typeof payload.output_text === "string" && payload.output_text.trim()) {
    return payload.output_text;
  }

  const output = Array.isArray(payload.output) ? payload.output : [];
  const texts = [];
  output.forEach((item) => {
    const content = Array.isArray(item.content) ? item.content : [];
    content.forEach((part) => {
      if (part?.type === "output_text" && typeof part.text === "string") {
        texts.push(part.text);
      }
    });
  });
  return texts.join("").trim();
}

function sanitizeRecommendations(parsed, savedRecipes) {
  const recipesById = new Map(savedRecipes.map((recipe) => [String(recipe.id || ""), recipe]));
  const incoming = Array.isArray(parsed?.recommendations) ? parsed.recommendations : [];

  return {
    recommendations: incoming
      .map((item, index) => sanitizeRecommendation(item, index, recipesById))
      .filter(Boolean)
      .slice(0, 5)
  };
}

function sanitizeRecommendation(item, index, recipesById) {
  if (!item || typeof item !== "object") {
    return null;
  }

  const recipeId = toStringValue(item.recipeId);
  const linkedRecipe = recipeId ? recipesById.get(recipeId) : null;
  const name = toStringValue(item.name);
  if (!name) {
    return null;
  }

  return {
    id: toStringValue(item.id) || `ai-${index + 1}-${slugify(name)}`,
    recipeId,
    name,
    description: toStringValue(item.description),
    score: toInteger(item.score, 50),
    matchedIngredients: toStringList(item.matchedIngredients),
    missingIngredients: toMissingIngredients(item.missingIngredients),
    offerMatches: toOfferMatches(item.offerMatches),
    rationale: toStringValue(item.rationale),
    imageUrl: toStringValue(item.imageUrl) || toStringValue(linkedRecipe?.imageUrl)
  };
}

function toMissingIngredients(value) {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => ({
      name: toStringValue(item?.name),
      amount: toStringValue(item?.amount),
      unit: toStringValue(item?.unit),
      category: toStringValue(item?.category) || "Sonstiges",
      isCore: Boolean(item?.isCore)
    }))
    .filter((item) => item.name);
}

function toOfferMatches(value) {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => ({
      offerId: toStringValue(item?.offerId),
      storeName: toStringValue(item?.storeName),
      offerTitle: toStringValue(item?.offerTitle),
      ingredientName: toStringValue(item?.ingredientName),
      priceLabel: toStringValue(item?.priceLabel)
    }))
    .filter((item) => item.offerId && item.ingredientName);
}

function toStringList(value) {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.map((entry) => toStringValue(entry)).filter(Boolean);
}

function toStringValue(value) {
  return typeof value === "string" ? value.trim() : "";
}

function toInteger(value, fallback) {
  if (typeof value === "number" && Number.isFinite(value)) {
    return Math.max(0, Math.min(99, Math.round(value)));
  }
  return fallback;
}

function slugify(value) {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}
